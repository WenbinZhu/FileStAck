package conformance.naming;

import java.io.*;

import test.*;
import common.*;
import storage.*;

/** Tests naming server <code>delete</code> method.

    <p>
    This test starts two storage servers, and registers them with the naming
    server. It then checks that the naming server correctly commands the storage
    servers to delete both files and directories, when the naming server is
    asked to do so. In order to ensure that the file is present on both storage
    servers, the naming server accesses it enough times to cause the file to be
    replicated.
 */
public class DeletionTest extends NamingTest
{
    /** Test notice. */
    public static final String      notice =
        "checking naming server delete method";
    /** Prerequisites. */
    public static final Class[]     prerequisites =
        new Class[] {ReplicationTest.class};

    /** Storage server. */
    private DeletionStorageServer   server1;
    /** Storage server. */
    private DeletionStorageServer   server2;

    // A file and a directory to be deleted, and two files in the directory.
    private final Path              delete_file = new Path("/file");
    private final Path              delete_directory = new Path("/directory");
    private final Path              dummy_file = new Path("/directory/file");
    private final Path              dummy_file2 = new Path("/directory/file2");

    /** Number of times to access the file for reading to ensure that it is
        replicated. */
    private static final int        ACCESS_COUNT = 30;

    /** Number of deletions that has occurred. */
    private int                     deletions;

    /** Performs the test. */
    @Override
    protected void perform() throws TestFailed
    {
        testBadArguments();

        // Ensure that the file to be deleted is replicated to the second
        // storage server.
        for(int access_counter = 0; access_counter < ACCESS_COUNT;
            ++access_counter)
        {
            try
            {
                service_stub.lock(delete_file, false);
                service_stub.unlock(delete_file, false);
            }
            catch(Throwable t)
            {
                throw new TestFailed("unable to access " + delete_file +
                                     " for reading", t);
            }
        }

        testDeletion(delete_file, 2);
        testDeletion(delete_directory, 2);
    }

    /** Commands the naming server to delete the given object, and then checks
        that the number of delete requests received by storage servers is equal
        to the number expected.

        @param path Path to the object to be deleted.
        @param expected_deletions Number of storage server delete requests
                                  expected to be caused by this deletion.
     */
    private void testDeletion(Path path, int expected_deletions)
        throws TestFailed
    {
        boolean     result;

        deletions = 0;

        // Issue the request to the naming server.
        try
        {
            result = service_stub.delete(path);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to delete " + path, t);
        }

        if(!result)
            throw new TestFailed("unable to delete " + path);

        // Check that the object has been deleted from all storage servers.
        if(deletions < expected_deletions)
        {
            throw new TestFailed(path + " was not successfully deleted from " +
                                 "storage servers: expected at least " +
                                 expected_deletions + " deletions, but got " +
                                 deletions);
        }
    }

    /** Checks that the <code>delete</code> method cannot be called with bad
        arguments.

        <p>
        This method checks that <code>delete</code> fails when given
        <code>null</code>, and when given a path to a non-existent object.
     */
    private void testBadArguments() throws TestFailed
    {
        // Call delete with null as argument.
        try
        {
            service_stub.delete(null);
            throw new TestFailed("delete method accepted null as argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("delete method threw unexpected exception " +
                                 "when given null as argument", t);
        }

        // Call delete with a non-existent file as argument.
        try
        {
            boolean result = service_stub.delete(new Path("/another_file"));

            if(result)
            {
                throw new TestFailed("delete method accepted non-existent " +
                                     "file as argument");
            }
            else
            {
                throw new TestFailed("delete method returned false when " +
                                     "given non-existent file as argument, " +
                                     "but should have thrown " +
                                     "FileNotFoundException");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("delete method threw unexpected exception " +
                                 "when given null as argument", t);
        }
    }

    /** Starts and registers the two storage servers. */
    @Override
    protected void initialize() throws TestFailed
    {
        super.initialize();

        try
        {
            server1 = new DeletionStorageServer();
            server1.start(registration_stub,
                          new Path[] {delete_file, dummy_file}, null);

            server2 = new DeletionStorageServer();
            server2.start(registration_stub, new Path[] {dummy_file2}, null);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start storage servers", t);
        }
    }

    /** Stops the storage servers. */
    @Override
    protected void clean()
    {
        super.clean();

        if(server1 != null)
        {
            server1.stop();
            server1 = null;
        }

        if(server2 != null)
        {
            server2.stop();
            server2 = null;
        }
    }

    /** Class of storage servers used in the test.

        <p>
        This type of storage server expects to be commanded to delete a file or
        a directory. When the command is received, it checks that the argument
        is valid, and that it is not a duplicate command for this storage
        server.
     */
    private class DeletionStorageServer extends TestStorageServer
    {
        /** Indicates that the storage server still hosts a copy of the file. */
        private boolean     hosts_file;
        /** Indicates that the storage server still hosts a copy of the
            directory. */
        private boolean     hosts_directory;

        /** Creates a <code>DeletionStorageServer</code>. */
        DeletionStorageServer()
        {
            super(DeletionTest.this);

            hosts_file = true;
            hosts_directory = true;
        }

        /** Accepts a delete request from the naming server.

            <p>
            This method checks that the path supplied with the request is valid,
            refers to either the file or the directory hosted on the server, and
            that the object has not yet been deleted. If all these conditions
            are met, the number of deletions is incremented. Otherwise, the test
            fails.

            @param path Path to the object to be deleted.
            @return <code>true</code> if and only if a valid object is deleted.
         */
        @Override
        public synchronized boolean delete(Path path)
        {
            // Check that the path is not null.
            if(path == null)
            {
                failure(new TestFailed("delete called with null as argument"));
                return false;
            }

            // Determine which item is being deleted, if any.
            boolean         valid_path = false;
            boolean         item_present = false;
            boolean         is_file = true;

            if(path.equals(delete_file))
            {
                valid_path = true;
                is_file = true;
                item_present = hosts_file;
            }
            if(path.equals(delete_directory))
            {
                valid_path = true;
                is_file = false;
                item_present = hosts_directory;
            }

            // If the path is to a valid item, mark it as deleted. Otherwise,
            // fail the test.
            if(valid_path)
            {
                if(item_present)
                {
                    // This statement must be synchronized because the other
                    // storage server may also increment deletions.
                    synchronized(DeletionTest.this)
                    {
                        ++deletions;
                    }

                    if(is_file)
                        hosts_file = false;
                    else
                        hosts_directory = false;

                    return true;
                }
                else
                {
                    failure(new TestFailed("server asked to delete " + path +
                                           " after it has already been " +
                                           "deleted"));
                    return false;
                }
            }

            // If the path is not to one of the items on this storage server,
            // fail the test.
            failure(new TestFailed("server asked to delete unexpected path " +
                                   path));
            return false;
        }

        /** Accepts replication requests and simulates their success. */
        @Override
        public boolean copy(Path file, Storage server)
        {
            return true;
        }
    }
}
