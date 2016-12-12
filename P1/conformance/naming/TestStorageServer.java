package conformance.naming;

import java.net.*;

import test.*;
import rmi.*;
import common.*;
import naming.*;
import storage.*;

/** Base class of storage servers used to test the naming server.

    <p>
    Instances of this class and its subclasses are connected to the naming
    server under test. Methods of this class can be overridden to monitor the
    behavior of the naming server. The default implementations of the methods
    throw <code>UnsupportedOperationException</code>, indicating that a call to
    any of the methods in the client or command interfaces is not expected
    during a test.
 */
class TestStorageServer implements Storage, Command
{
    /** Test which is using this storage server. Used to signal failures. */
    protected final Test            test;
    /** Client interface skeleton. */
    private final StorageSkeleton   client_skeleton;
    /** Command interface skeleton. */
    private final CommandSkeleton   command_skeleton;
    /** Client interface stub. */
    protected Storage               client_stub;
    /** Command interface stub. */
    protected Command               command_stub;
    /** Indicates that the skeletons have been started. This is used to prevent
        multiple starts of the skeletons by alternative implementations of the
        <code>start</code> method. */
    private boolean                 skeletons_started;

    /** Creates the test storage server.

        @param test The test which is using this storage server.
     */
    TestStorageServer(Test test)
    {
        this.test = test;
        client_skeleton = new StorageSkeleton();
        command_skeleton = new CommandSkeleton();
        skeletons_started = false;
    }

    /** Starts skeletons for the client and command interfaces.

        <p>
        The naming server registration test has alternative implementations of
        the <code>start</code> method. Each such implementation calls this
        method to prevent repeated calls to <code>start</code> in the skeletons.

        @throws RMIException If either skeleton cannot be started.
        @throws UnknownHostException If a stub cannot be created from either of
                                     the skeletons.
     */
    protected synchronized void startSkeletons()
        throws RMIException, UnknownHostException
    {
        // Prevent repeated starting of the skeletons and re-creation of stubs.
        if(skeletons_started)
            return;

        // Start the client interface skeleton and create the stub.
        client_skeleton.start();
        client_stub = Stub.create(Storage.class, client_skeleton);

        // Start the registration skeleton and create the stub.
        command_skeleton.start();
        command_stub = Stub.create(Command.class, command_skeleton);

        skeletons_started = true;
    }

    /** Starts the test storage server.

        <p>
        The storage server connects to the given naming server and attempts to
        register. It provides the naming server with the given list of files.
        If <code>expect_files</code> is not <code>null</code>, the storage
        server checks that the naming server then asks it to delete that set of
        files.

        @param naming_server The naming server with which the storage server is
                             to register.
        @param offer_files The list of files which is to be registered with the
                           naming server.
        @param expect_files An optional list of files which the storage server
                            expects the naming server to ask it to delete. This
                            parameter can be <code>null</code> if the storage
                            server should not check which files the naming
                            server replies with.
        @return The storage server client interface stub.
        @throws RMIException If the storage server skeletons cannot be started,
                             or if the storage server is unable to register due
                             to a network error.
        @throws UnknownHostException If one of the storage server stubs cannot
                                     be created.
        @throws TestFailed If <code>expect_files</code> is not <code>null</code>
                           and the naming server does not reply with the
                           correct set of duplicate files.
     */
    synchronized Storage start(Registration naming_server, Path[] offer_files,
                               Path[] expect_files)
        throws RMIException, UnknownHostException, TestFailed
    {
        // Start storage server skeletons.
        startSkeletons();

        // Register the storage server with the naming server.
        Path[]      delete_files =
            naming_server.register(client_stub, command_stub, offer_files);

        // Check that the naming server replied with the approprite files.
        if(expect_files != null)
        {
            if(!TestUtil.sameElements(delete_files, expect_files))
            {
                throw new TestFailed("naming server did not command deletion " +
                                     "of the expected files");
            }
        }

        return client_stub;
    }

    /** Stops the storage server. */
    synchronized void stop()
    {
        client_skeleton.stop();
        command_skeleton.stop();
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    @Override
    public long size(Path file)
    {
        test.failure(new TestFailed("unexpected call to size method in " +
                                    "storage server"));

        throw new UnsupportedOperationException("size method not implemented");
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    @Override
    public byte[] read(Path file, long offset, int length)
    {
        test.failure(new TestFailed("unexpected call to read method in " +
                                    "storage server"));

        throw new UnsupportedOperationException("read method not implemented");
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    @Override
    public void write(Path file, long offset, byte[] data)
    {
        test.failure(new TestFailed("unexpected call to write method in " +
                                    "storage server"));

        throw new UnsupportedOperationException("write method not implemented");
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    @Override
    public boolean create(Path file)
    {
        test.failure(new TestFailed("unexpected call to create method in " +
                                    "storage server"));

        throw new UnsupportedOperationException("create method not implemented");
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    @Override
    public boolean delete(Path file)
    {
        test.failure(new TestFailed("unexpected call to delete method in " +
                                    "storage server"));

        throw new UnsupportedOperationException("delete method not " +
                                                "implemented");
    }

    /** Client interface skeleton.

        <p>
        This class overrides the <code>listen_error</code> and
        <code>service_error</code> to fail the test when a top-level exception
        occurs in either the listening thread or one of the service threads.
     */
    private class StorageSkeleton extends Skeleton<Storage>
    {
        /** Creates the client interface skeleton. */
        StorageSkeleton()
        {
            super(Storage.class, TestStorageServer.this);
        }

        /** Fails the test when an exception occurs in the listening thread. */
        @Override
        protected boolean listen_error(Exception e)
        {
            test.failure(new TestFailed("caught exception in storage " +
                                        "skeleton listening thread", e));

            return false;
        }

        /** Fails the test when an exception occurs in a service thread. */
        @Override
        protected void service_error(RMIException e)
        {
            test.failure(new TestFailed("caught exception in storage " +
                                        "skeleton service thread", e));
        }
    }

    /** Command interface skeleton.

        <p>
        This class overrides the <code>listen_error</code> and
        <code>service_error</code> to fail the test when a top-level exception
        occurs in either the listening thread or one of the service threads.
     */
    private class CommandSkeleton extends Skeleton<Command>
    {
        /** Creates the command interface skeleton. */
        CommandSkeleton()
        {
            super(Command.class, TestStorageServer.this);
        }

        /** Fails the test when an exception occurs in the listening thread. */
        @Override
        protected boolean listen_error(Exception e)
        {
            test.failure(new TestFailed("caught exception in command " +
                                        "skeleton listening thread", e));

            return false;
        }

        /** Fails the test when an exception occurs in a service thread. */
        @Override
        protected void service_error(RMIException e)
        {
            test.failure(new TestFailed("caught exception in command " +
                                        "skeleton service thread", e));
        }
    }
}
