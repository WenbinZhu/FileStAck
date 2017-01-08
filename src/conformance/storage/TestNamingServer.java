package conformance.storage;

import test.*;
import rmi.*;
import common.*;
import storage.*;
import naming.*;

/** Test naming server.

    <p>
    This naming server performs the following checks each time a storage server
    registers:
    <ul>
    <li>None of the arguments to <code>register</code> are
        <code>null</code>.</li>
    <li>The correct file list has been sent.</li>
    </ul>
 */
class TestNamingServer implements naming.Registration
{
    /** Test object that is running this server. */
    private final Test          test;
    /** List of paths to expect from the next storage server to register. */
    private Path[]              expect_files = null;
    /** List of paths to command the next storage server to register to
        delete. */
    private Path[]              delete_files = null;
    /** Skeleton front for this server. */
    private final TestSkeleton  skeleton;
    /** Last registered storage server client interface. */
    private Storage             client_stub = null;
    /** Last registered storage server command interface. */
    private Command             command_stub = null;
    /** Number of storage servers registered with the naming server. */
    private int                 storage_servers = 0;
    /** Indicates that the skeleton has stopped. */
    private boolean             stopped = false;

    /** Creates the test naming server.

        @param test The test which is running this naming server.
     */
    TestNamingServer(Test test)
    {
        this.test = test;
        this.skeleton = new TestSkeleton();
    }

    /** Sets the files the next storage server to connect is expected to
        register.

        @param files The files to expect. The naming server will check that
                     these are indeed the files that are received. If this
                     argument is <code>null</code>, the naming server will not
                     perform the check.
     */
    public void expectFiles(Path[] files)
    {
        expect_files = files;
    }

    /** Sets the files the next storage server to connect will be commanded to
        delete.

        @param files The files to be deleted. If this argument is
        <code>null</code>, the naming server will not command the storage server
        to delete any files.
     */
    public void deleteFiles(Path[] files)
    {
        delete_files = files;
    }

    /** Returns the client interface for the last storage server to register. */
    public Storage clientInterface()
    {
        return client_stub;
    }

    /** Returns the command interface for the last storage server to
        register. */
    public Command commandInterface()
    {
        return command_stub;
    }

    // Detailed documentation in Registration.java.
    @Override
    public synchronized Path[] register(Storage client_stub,
                                        Command command_stub, Path[] files)
        throws RMIException
    {
        // Ensure that none of the arguments are null.
        if(client_stub == null)
        {
            test.failure(new TestFailed("storage server client interface " +
                                        "null during registration"));
        }

        if(command_stub == null)
        {
            test.failure(new TestFailed("storage server command interface " +
                                        "null during registration"));
        }

        if(files == null)
        {
            test.failure(new TestFailed("files array null during " +
                                        "registration"));
        }

        // If expect_files is not null, make sure that the files list received
        // is the same as the files list expected.
        if(expect_files != null)
        {
            if(!TestUtil.sameElements(files, expect_files))
            {
                test.failure(new TestFailed("received wrong file list during " +
                                            "registration"));
            }
        }

        // Set the stubs for the newly-registered server.
        this.client_stub = client_stub;
        this.command_stub = command_stub;

        // If delete_files is not null, return the list of files to delete.
        // Otherwise, the server is not to delete anything.
        if(delete_files != null)
            return delete_files;
        else
            return new Path[0];
    }

    /** Retrieves a registration stub for the test server.

        @return The stub.
        @throws TestFailed If a stub cannot be obtained.
     */
    Registration stub() throws TestFailed
    {
        try
        {
            return Stub.create(Registration.class, skeleton);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create registration stub", t);
        }
    }

    /** Starts the test naming server.

        @throws TestFailed If the test cannot be started.
     */
    void start() throws TestFailed
    {
        try
        {
            skeleton.start();
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start naming server", t);
        }
    }

    /** Stops the test naming server. */
    void stop()
    {
        skeleton.stop();

        // Wait for the skeleton to stop before exiting.
        synchronized(this)
        {
            while(!stopped)
            {
                try
                {
                    wait();
                }
                catch(InterruptedException e) { }
            }
        }
    }

    /** Skeleton front for the test naming server. */
    private class TestSkeleton extends Skeleton<Registration>
    {
        /** Creates the skeleton. */
        TestSkeleton()
        {
            super(Registration.class, TestNamingServer.this);
        }

        /** Sets the <code>stopped</code> flag and wakes any thread waiting in
            the server's <code>stop</code> method. */
        @Override
        protected void stopped(Throwable cause)
        {
            synchronized(TestNamingServer.this)
            {
                stopped = true;
                TestNamingServer.this.notifyAll();
            }
        }

        /** Fails the test upon an exception in the listening thread. */
        @Override
        protected boolean listen_error(Exception e)
        {
            test.failure(new TestFailed("caught exception in naming server " +
                                        "listening thread", e));

            return false;
        }

        /** Fails the test upon an exception in the service thread. */
        @Override
        protected void service_error(RMIException e)
        {
            test.failure(new TestFailed("caught exception in naming server " +
                                        "service thread", e));
        }
    }
}
