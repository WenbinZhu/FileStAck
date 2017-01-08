package conformance.naming;

import java.io.*;

import test.*;
import common.*;

/** Tests file and directory creation.

    <p>
    This test starts a naming server and a test storage server. Items checked
    are:
    <ul>
    <li><code>createFile</code> and <code>createDirectory</code> reject bad
        arguments such as <code>null</code> pointers, paths to existing objects,
        and paths to objects whose parents do not exist.</li>
    <li><code>createDirectory</code> correctly creates directories on the
        naming server.
    <li><code>createFile</code> correctly creates files on the naming server,
        and new files are then created on the connected storage server. The test
        does not succeed until the storage server has received the request to
        create the file on its local filesystem.
    </ul>
 */
public class CreationTest extends NamingTest
{
    /** Test notice. */
    public static final String  notice =
        "checking naming server creation methods (createFile, createDirectory)";
    /** Prerequisites. */
    public static final Class[] prerequisites = new Class[] {ListingTest.class};

    /** Storage server used in the test. */
    private CreationTestStorageServer   storage_server;

    /** File that the storage server is expecting to be commanded to create by
        the naming server. */
    private Path                        expect_file = null;
    /** Indicates that the file <code>expect_file</code> has been created on the
        storage server by command from the naming server. */
    private boolean                     file_created;

    /** Indicates that all wait loops should be terminated. */
    private boolean                     wake_all = false;

    /** Creates the <code>CreationTest</code> and sets the notice. */
    public CreationTest()
    {
        storage_server = new CreationTestStorageServer();
    }

    /** Runs all the tests.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testCreateDirectoryArguments();
        testCreateFileArguments();

        testDirectoryCreation(new Path("/directory/subdirectory"));
        testDirectoryCreation(new Path("/another_directory"));

        testFileCreation(new Path("/file"));
        testFileCreation(new Path("/another_directory/file"));
    }

    /** Tests that valid requests to create a file cause a file to be created on
        the naming server and on the storage server.

        @param file The path to the file to be created.
        @throws TestFailed If the file is not created on either the naming or
                           the storage server.
     */
    private void testFileCreation(Path file) throws TestFailed
    {
        // Set fields that will be used by the storage server to determine if it
        // has received the correct file creation request.
        expect_file = file;
        file_created = false;

        boolean     result;

        // Attempt to create the file.
        try
        {
            result = service_stub.createFile(file);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create new file " + file, t);
        }

        if(!result)
            throw new TestFailed("unable to create new file");

        // Check that the file is reported as a file.
        try
        {
            result = service_stub.isDirectory(file);
        }
        catch(FileNotFoundException e)
        {
            throw new TestFailed("naming server failed to create file " + file,
                                 e);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unexpected exception when checking file " +
                                 file, t);
        }

        if(result)
        {
            throw new TestFailed("new file " + file + " reported as a " +
                                 "directory");
        }

        // Do not continue until the file has been created on the storage
        // server. This ensures that the test will be failed by timeout if the
        // request to create the file is not received by the storage server.
        // Typically, the file has already been created, as this is done
        // synchronously before createFile returns.
        task("waiting for command to create " + file + " on the storage " +
             "server");

        synchronized(this)
        {
            while(!file_created && !wake_all)
            {
                try
                {
                    wait();
                }
                catch(InterruptedException e) { }
            }
        }

        task();

        // The storage server should not expect further requests to create a
        // file for now.
        expect_file = null;
    }

    /** Tests that valid requests to create a directory cause a directory to be
        created on the naming server.

        @param directory Path to the directory.
        @throws TestFailed If the directory is not created.
     */
    private void testDirectoryCreation(Path directory) throws TestFailed
    {
        // Attempt to create the directory.
        boolean     result;

        try
        {
            result = service_stub.createDirectory(directory);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create new directory " + directory,
                                 t);
        }

        if(!result)
            throw new TestFailed("unable to create new directory " + directory);

        // Check that the object created is indeed a directory.
        try
        {
            result = service_stub.isDirectory(directory);
        }
        catch(FileNotFoundException e)
        {
            throw new TestFailed("naming server failed to create directory " +
                                 directory, e);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unexpected exception when checking " +
                                 "directory " + directory, t);
        }

        if(!result)
        {
            throw new TestFailed("new directory " + directory + " reported " +
                                 "as a file");
        }
    }

    /** Checks that the <code>createDirectory</code> method rejects invalid
        arguments.

        @throws TestFailed If the test fails.
     */
    private void testCreateDirectoryArguments() throws TestFailed
    {
        // Check that createDirectory rejects null.
        try
        {
            service_stub.createDirectory(null);
            throw new TestFailed("createDirectory accepted null as argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("createDirectory threw unexpected exception " +
                                 "when given null as argument", t);
        }

        // Check that createDirectory rejects paths whose parent directories do
        // not exist.
        try
        {
            service_stub.createDirectory(new Path("/directory2/subdirectory"));
            throw new TestFailed("createDirectory accepted new directory " +
                                 "whose parent directory does not exist");
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("createDirectory threw unexpected exception " +
                                 "when given new directory whose parent " +
                                 "directory does not exist", t);
        }

        // Check that createDirectory rejects paths whose parent directories are
        // in fact files.
        try
        {
            service_stub.createDirectory(new Path("/directory/file/directory"));
            throw new TestFailed("createDirectory accepted new directory " +
                                 "whose parent directory is a file");
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("createDirectory threw unexpected exception " +
                                 "when given new directory whose parent " +
                                 "directory is a file", t);
        }

        // Check that createDirectory rejects paths to existing directories.
        boolean     result;

        try
        {
            result = service_stub.createDirectory(new Path("/directory"));
        }
        catch(Throwable t)
        {
            throw new TestFailed("createDirectory threw unexpected exception " +
                                 "when given a path to an existing directory",
                                 t);
        }

        if(result)
        {
            throw new TestFailed("createDirectory accepted a path to an " +
                                 "existing directory");
        }

        // Check that createDirectory rejects paths to existing files.
        try
        {
            result = service_stub.createDirectory(new Path("/directory/file"));
        }
        catch(Throwable t)
        {
            throw new TestFailed("createDirectory threw unexpected exception " +
                                 "when given a path to a file", t);
        }

        if(result)
            throw new TestFailed("createDirectory accepted a path to a file");

        // Check that createDirectory rejects the root directory as an
        // argument.
        try
        {
            result = service_stub.createDirectory(new Path("/"));
        }
        catch(Throwable t)
        {
            throw new TestFailed("createDirectory threw unexpected exception " +
                                 "when given the root directory", t);
        }

        if(result)
            throw new TestFailed("createDirectory accepted the root directory");
    }

    /** Checks that the <code>createFile</code> method rejects invalid
        arguments.

        @throws TestFailed If the test fails.
     */
    private void testCreateFileArguments() throws TestFailed
    {
        // Check that createFile rejects null.
        try
        {
            service_stub.createFile(null);
            throw new TestFailed("createFile accepted null as argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("createFile threw unexpected exception when " +
                                 "given null as argument", t);
        }

        // Check that createFile rejects paths whose parent directories do not
        // exist.
        try
        {
            service_stub.createFile(new Path("/directory2/file"));
            throw new TestFailed("createFile accepted path to new file whose " +
                                 "parent directory does not exist");
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("createFile threw unexpected exception when " +
                                 "given path to new file whose parent " +
                                 "directory does not exist", t);
        }

        // Check that createFile rejects paths whose parent directories are in
        // fact files.
        try
        {
            service_stub.createFile(new Path("/directory/file/file"));
            throw new TestFailed("createFile accepted new file whose parent " +
                                 "directory is a file");
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("createFile threw unexpected exception " +
                                 "when given new file whose parent directory " +
                                 "is a file", t);
        }

        // Check that createFile rejects paths to existing files.
        boolean     result;

        try
        {
            result = service_stub.createFile(new Path("/directory/file"));
        }
        catch(Throwable t)
        {
            throw new TestFailed("createFile threw unexpected exception when " +
                                 "given a path to an existing file", t);
        }

        if(result)
        {
            throw new TestFailed("createFile accepted a path to an existing" +
                                 "file");
        }

        // Check that createFile rejects paths to existing directories.
        try
        {
            result = service_stub.createFile(new Path("/directory"));
        }
        catch(Throwable t)
        {
            throw new TestFailed("createFile threw unexpected exception when " +
                                 "given a path to a directory", t);
        }

        if(result)
            throw new TestFailed("createFile accepted a path to a directory");

        // Check that createFile rejects the root directory.
        try
        {
            result = service_stub.createFile(new Path("/"));
        }
        catch(Throwable t)
        {
            throw new TestFailed("createFile threw unexpected exception when " +
                                 "given the root directory", t);
        }

        if(result)
            throw new TestFailed("createFile accepted the root directory");
    }

    /** Starts servers used in the test.

        @throws TestFailed If the servers cannot be started.
     */
    @Override
    protected void initialize() throws TestFailed
    {
        super.initialize();

        try
        {
            storage_server.start(registration_stub,
                                 new Path[] {new Path("/directory/file")},
                                 null);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start storage server", t);
        }
    }

    /** Stops all servers used in the test and unblocks any waiting threads. */
    @Override
    protected void clean()
    {
        super.clean();

        if(storage_server != null)
        {
            storage_server.stop();
            storage_server = null;
        }

        synchronized(this)
        {
            wake_all = true;
            notifyAll();
        }
    }

    /** Storage server for the creation test.

        <p>
        This storage server waits for calls to <code>create</code> on its
        command interface. If an expected call is received, it wakes any thread
        that may be blocked in <code>testFileCreation</code>. If no such call
        is expected, or the call is received with the wrong argument, the server
        fails the test.
     */
    private class CreationTestStorageServer extends TestStorageServer
    {
        /** Creates the test storage server. */
        CreationTestStorageServer()
        {
            super(CreationTest.this);
        }

        /** Checks that the naming server has commanded the correct file to be
            created, if such a command is expected. */
        @Override
        public boolean create(Path file)
        {
            // If no file creation request is expected, behave as the superclass
            // - fail the test. Otherwise, check that the file argument is not
            // null and that the correct path has been provided.
            if(expect_file == null)
                super.create(file);
            else
            {
                if(file == null)
                {
                    failure(new TestFailed("create method called with null " +
                                           "as argument"));
                    throw new NullPointerException();
                }

                if(!file.equals(expect_file))
                {
                    failure(new TestFailed("create method called with wrong " +
                                           "argument: expected " + expect_file +
                                           ", but got " + file));
                    throw new IllegalArgumentException();
                }

                synchronized(CreationTest.this)
                {
                    file_created = true;
                    CreationTest.this.notifyAll();
                }

                return true;
            }

            return false;
        }
    }
}
