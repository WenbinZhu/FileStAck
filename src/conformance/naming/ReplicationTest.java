package conformance.naming;

import test.*;
import common.*;
import storage.*;

/** Tests naming server replication policy.

    <p>
    This test registers a storage server with a single file, and a second
    storage server with no files. It then accesses the file a large number of
    times. By the time all the accesses are complete, the naming server should
    have issued a command to the second server to copy the file from the first
    server. The "accesses" are performed by locking the file for shared access.

    <p>
    After testing replication in the manner described above, the test then
    locks the file for exclusive access. The file should now be deleted from
    exactly one of the two storage servers.
 */
public class ReplicationTest extends NamingTest
{
    /** Test notice. */
    public static final String          notice =
        "checking naming server replication policy";
    /** Prerequisites. */
    public static final Class[]         prerequisites =
        new Class[] {LockTest.class};

    /** Storage server hosting the initial copy of the file. */
    private InvalidationStorageServer   hosting_server = null;
    /** Storage server receiving the replicated copy of the file. */
    private MirrorStorageServer         mirror_server = null;
    /** Stub for the storage server initially hosting the file. The other server
        ensures that the naming server commands the copy to be done from this
        server. */
    private Storage                     hosting_stub;

    /** File to be replicated. */
    private final Path                  replicate_file = new Path("/file");
    /** Indicates that the file has been replicated. */
    private boolean                     replicated = false;
    /** Indicates that the file has been invalidated. */
    private boolean                     invalidated = false;
    /** Indicates that all waiting threads should terminate. */
    private boolean                     wake_all = false;
    /** Number of shared access requests to make in order to cause the file to
        be replicated. */
    private static final int            ACCESS_COUNT = 30;

    /** Performs the test. */
    @Override
    protected void perform() throws TestFailed
    {
        int     access_counter;

        // Access the file to be replicated a large number of times for reading.
        for(access_counter = 0; access_counter < ACCESS_COUNT; ++access_counter)
        {
            try
            {
                service_stub.lock(replicate_file, false);
            }
            catch(Throwable t)
            {
                throw new TestFailed("unable to lock " + replicate_file +
                                     " for reading", t);
            }

            try
            {
                service_stub.unlock(replicate_file, false);
            }
            catch(Throwable t)
            {
                throw new TestFailed("unable to unlock " + replicate_file, t);
            }
        }

        // Set test task message.
        task("waiting for " + replicate_file + " to be replicated");

        // Wait until the file is replicated. This wait allows some leeway for
        // asynchronous replication.
        synchronized(this)
        {
            while(!replicated && !wake_all)
            {
                try
                {
                    wait();
                }
                catch(InterruptedException e) { }
            }
        }

        // Clear test task message.
        task();

        // The file should not have been invalidated.
        if(invalidated)
            throw new TestFailed(replicate_file + " invalidated prematurely");

        // Access the file for writing.
        try
        {
            service_stub.lock(replicate_file, true);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to lock " + replicate_file + " for " +
                                 "writing", t);
        }

        try
        {
            service_stub.unlock(replicate_file, true);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to unlock " + replicate_file, t);
        }

        // Wait for the file to be invalidated.
        task("waiting for " + replicate_file + " to be invalidated");

        synchronized(this)
        {
            while(!invalidated && !wake_all)
            {
                try
                {
                    wait();
                }
                catch(InterruptedException e) { }
            }
        }

        // Clear test task message.
        task();
    }

    /** Starts the two storage servers. */
    @Override
    protected void initialize() throws TestFailed
    {
        super.initialize();

        try
        {
            hosting_server = new InvalidationStorageServer();
            hosting_stub =
                hosting_server.start(registration_stub,
                                     new Path[] {replicate_file}, null);

            mirror_server = new MirrorStorageServer();
            mirror_server.start(registration_stub, new Path[] {}, null);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start storage server", t);
        }
    }

    /** Stops the storage servers and wakes any waiting threads. */
    @Override
    protected void clean()
    {
        super.clean();

        if(hosting_server != null)
        {
            hosting_server.stop();
            hosting_server = null;
        }

        if(mirror_server != null)
        {
            mirror_server.stop();
            mirror_server = null;
        }

        synchronized(this)
        {
            wake_all = true;
            notifyAll();
        }
    }

    /** Storage server expecting a request to copy the file. */
    private class MirrorStorageServer extends InvalidationStorageServer
    {
        /** Checks that a proper request to copy the file has been received.

            <p>
            Neither of the arguments may be <code>null</code>. The path to the
            file to be copied must be the path expected, and the storage server
            from which to copy the file must be other storage server in the
            test. There must not be duplicate requests to copy the file. If any
            of these conditions is violated, the test fails.

            @param file Path to the file to be copied.
            @param server Stub for the storage server from which to copy the
                          file.
         */
        @Override
        public boolean copy(Path file, Storage server)
        {
            // Check that neither argument is null.
            if(file == null)
            {
                failure(new TestFailed("file argument to copy method null"));
                return false;
            }

            if(server == null)
            {
                failure(new TestFailed("server stub argument to copy method " +
                                       "null"));
                return false;
            }

            // Check that the request is for the correct file.
            if(!file.equals(replicate_file))
            {
                failure(new TestFailed("naming server requested " + file +
                                       " to be copied, but " + replicate_file +
                                       " was expected"));
                return false;
            }

            // Check that the request gives the correct server.
            if(!server.equals(hosting_stub))
            {
                failure(new TestFailed("naming server provided the wrong " +
                                       "storage server stub"));
                return false;
            }

            // Check that this is the first replication request. If so, note the
            // request and wake any waiting thread.
            synchronized(ReplicationTest.this)
            {
                if(replicated)
                {
                    failure(new TestFailed("duplicate request to copy " +
                                           file));
                    return false;
                }

                replicated = true;
                ReplicationTest.this.notifyAll();
            }

            return true;
        }
    }

    /** Storage server expecting to be (potentially) notified that the file may
        be deleted.

        <p>
        Both of the storage servers in this test are instances of this class.
        Exactly one should receive a request to delete the file, after the file
        has been replicated and is then accessed for writing.
     */
    private class InvalidationStorageServer extends TestStorageServer
    {
        /** Creates the <code>InvalidationStorageServer</code>. */
        InvalidationStorageServer()
        {
            super(ReplicationTest.this);
        }

        /** Checks that a delete request is received for the proper file.

            <p>
            The path must not be <code>null</code>, and must be the path to the
            proper file. There must be only one invalidation request.

            @param path Path to the file to be deleted.
         */
        @Override
        public boolean delete(Path path)
        {
            // Check that the path is correct.
            if(path == null)
            {
                failure(new TestFailed("path argument to delete method null"));
                return false;
            }

            if(!path.equals(replicate_file))
            {
                failure(new TestFailed("naming server requested deletion of " +
                                       path + ", but deletion of " +
                                       replicate_file + " is expected"));
                return false;
            }

            // Check that there has not yet been an invalidaton request. If this
            // is so, wake any waiting threads. Note that a second invalidation
            // request may come after the test has already been marked as
            // successful by the return of the main thread from its perform
            // method. There is no sure way to check that this does not occur,
            // as the naming server may send a duplicate invalidation request
            // asynchronously to the other (or the same) storage server.
            synchronized(ReplicationTest.this)
            {
                if(invalidated)
                {
                    failure(new TestFailed("more than one server requested " +
                                           "delete " + replicate_file));
                }

                invalidated = true;
                ReplicationTest.this.notifyAll();
            }

            return true;
        }
    }
}
