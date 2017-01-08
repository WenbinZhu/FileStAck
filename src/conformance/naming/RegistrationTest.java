package conformance.naming;

import test.*;
import common.*;
import storage.*;

/** Tests the naming server <code>register</code> method.

    <p>
    The following items are checked:
    <ul>
    <li>The naming server rejects <code>null</code> pointers as arguments.</li>
    <li>Duplicate registrations are rejected.</li>
    <li>The naming server correctly commands the storage server to delete
        duplicate files.</li>
    <li>The naming server correctly commands the storage server to delete files
        that shadow a directory on the naming server.</li>
    <li>Attempts to register the root directory as a file are silently ignored
        by the naming server.</li>
    </ul>
 */
public class RegistrationTest extends NamingTest
{
    /** Test notice. */
    public static final String  notice =
        "checking naming server registration interface";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {conformance.common.PathTest.class, ContactTest.class};

    /** First storage server to register. */
    private TestStorageServer               server1;
    /** Storage server registering duplicate files. */
    private TestStorageServer               server2;
    /** Storage server registering files that shadow a directory. */
    private TestStorageServer               server3;
    /** Storage server attempting to register the root directory. */
    private TestStorageServer               server4;

    /** Storage server used for the <code>null</code> pointer and duplicate
        registration tests. */
    private UnsuccessfulStorageServer       badServer;

    /** Creates the <code>RegistrationTest</code> object. */
    public RegistrationTest()
    {
        server1 = new TestStorageServer(this);
        server2 = new TestStorageServer(this);
        server3 = new TestStorageServer(this);
        server4 = new TestStorageServer(this);
        badServer = new UnsuccessfulStorageServer();
    }

    /** Performs the tests.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        synchronized(this)
        {
            badServer.testNullPointers();
        }

        synchronized(this)
        {
            badServer.testDuplicateRegistration();
        }

        testMerging();
    }

    /** Performs several registrations that should succeed, and checks that the
        files chosen by the naming server for deletion are correct.

        @throws TestFailed If any of the tests fail.
     */
    private void testMerging() throws TestFailed
    {
        // Register the first storage server with four files.
        Path[]      server1_files =
            new Path[] {new Path("/file"),
                        new Path("/directory/file"),
                        new Path("/directory/another_file"),
                        new Path("/another_directory/file")};

        try
        {
            synchronized(this)
            {
                server1.start(registration_stub, server1_files, null);
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to register storage server with " +
                                 "naming server", t);
        }

        // Register the second storage server with three files. Two of them were
        // registered by the first server, and are therefore duplicates. The
        // naming server should request that these files be deleted by the
        // second storage server.
        Path[]      server2_files =
            new Path[] {new Path("/file"),
                        new Path("/directory/file"),
                        new Path("/another_directory/another_file")};
        Path[]      server2_delete_files =
            new Path[] {new Path("/file"),
                        new Path("/directory/file")};

        try
        {
            synchronized(this)
            {
                server2.start(registration_stub, server2_files,
                              server2_delete_files);
            }
        }
        catch(TestFailed e)
        {
            throw new TestFailed("naming server did not command deletion " +
                                 "of the expected files when checking " +
                                 "regular files");
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to register second storage server " +
                                 "with naming server", t);
        }

        // Register the third storage server with a file that shadows a
        // directory on the naming server. The naming server should command this
        // file to be deleted.
        Path[]      server3_files =
            new Path[] {new Path("/directory"),
                        new Path("/another_file")};
        Path[]      server3_delete_files = new Path[] {new Path("/directory")};

        try
        {
            synchronized(this)
            {
                server3.start(registration_stub, server3_files,
                              server3_delete_files);
            }
        }
        catch(TestFailed e)
        {
            throw new TestFailed("naming server did not command deletion " +
                                 "of the expected files when checking a file " +
                                 "that overlaps a directory");
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to register third storange server " +
                                 "with naming server", t);
        }

        // Register the fourth storage server with the root directory among its
        // list of files. The naming server should silently ignore this attempt.
        Path[]      server4_files = new Path[] {new Path("/")};
        Path[]      server4_delete_files = new Path[0];

        try
        {
            synchronized(this)
            {
                server4.start(registration_stub, server4_files,
                              server4_delete_files);
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("naming server did not silently ignore " +
                                 "request to add root directory as a file", t);
        }
    }

    /** Stops all servers started by the test. */
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

        if(server3 != null)
        {
            server3.stop();
            server3 = null;
        }

        if(server4 != null)
        {
            server4.stop();
            server4 = null;
        }

        if(badServer != null)
        {
            badServer.stop();
            badServer = null;
        }
    }

    /** Storage server used for the <code>null</code> pointer and duplicate
        registration tests. */
    private class UnsuccessfulStorageServer extends TestStorageServer
    {
        /** Create the storage server. */
        UnsuccessfulStorageServer()
        {
            super(RegistrationTest.this);
        }

        /** Tests that the naming server rejects repeat registrations of the
            same storage server.

            <p>
            This method is a replacement for the <code>start</code> method of
            the storage server.

            @throws TestFailed If the naming server accepts the second
                               registration.
         */
        synchronized void testDuplicateRegistration() throws TestFailed
        {
            // Start the storage server skeletons, if they have not already been
            // started.
            try
            {
                startSkeletons();
            }
            catch(Throwable t)
            {
                throw new TestFailed("unable to start skeletons for " +
                                     "duplicate registration test", t);
            }

            // Register the storage server with the naming server. This
            // registration should succeed.
            try
            {
                registration_stub.register(client_stub, command_stub,
                                           new Path[0]);
            }
            catch(IllegalStateException e)
            {
                throw new TestFailed("storage server reported as already " +
                                     "registered during initial registration " +
                                     "in duplicate registration test: " +
                                     "perhaps it was mistakenly registered " +
                                     "during the invalid reference test", e);
            }
            catch(Throwable t)
            {
                throw new TestFailed("unable to perform initial registration " +
                                     "of storage server in duplicate " +
                                     "registration test", t);
            }

            // Attempt to register the storage server with the naming server a
            // second time. The test is failed if this registration succeeds.
            try
            {
                registration_stub.register(client_stub, command_stub,
                                           new Path[0]);
                throw new TestFailed("naming server accepted duplicate " +
                                     "registration");
            }
            catch(TestFailed e) { throw e; }
            catch(IllegalStateException e) { }
            catch(Throwable t)
            {
                throw new TestFailed("register method threw unexpected " +
                                     "exception during duplicate " +
                                     "registration", t);
            }
        }

        /** Tests that the naming server <code>register</code> method rejects
            <code>null</code> arguments.

            <p>
            This method is a replacement for the <code>start</code> method of
            the storage server.

            @throws TestFailed If the <code>register</code> method accepts
                               <code>null</code> as an argument for any of the
                               formal parameters.
         */
        synchronized void testNullPointers() throws TestFailed
        {
            // Start the storage server skeletons, if they have not already been
            // started.
            try
            {
                startSkeletons();
            }
            catch(Throwable t)
            {
                throw new TestFailed("unable to start skeletons for invalid " +
                                     "reference test", t);
            }

            // Attempt to register with null as the client interface stub.
            try
            {
                registration_stub.register(null, command_stub, new Path[0]);
                throw new TestFailed("register method accepted null for " +
                                     "storage server client interface " +
                                     "argument");
            }
            catch(TestFailed e) { throw e; }
            catch(NullPointerException e) { }
            catch(Throwable t)
            {
                throw new TestFailed("register method threw unexpected " +
                                     "exception when given null for storage " +
                                     "server client interface argument", t);
            }

            // Attempt to register with null as the command interface stub.
            try
            {
                registration_stub.register(client_stub, null, new Path[0]);
                throw new TestFailed("register method accepted null for " +
                                     "storage server command interface " +
                                     "argument");
            }
            catch(TestFailed e) { throw e; }
            catch(NullPointerException e) { }
            catch(Throwable t)
            {
                throw new TestFailed("register method threw unexpected " +
                                     "exception when given null for storage " +
                                     "server command interface argument", t);
            }

            // Attempt to register with null as the file list.
            try
            {
                registration_stub.register(client_stub, command_stub, null);
                throw new TestFailed("register method accepted null for " +
                                     "path argument");
            }
            catch(TestFailed e) { throw e; }
            catch(NullPointerException e) { }
            catch(Throwable t)
            {
                throw new TestFailed("register method threw unexpected " +
                                     "exception when given null for path " +
                                     "argument", t);
            }
        }
    }
}
