package conformance.storage;

import rmi.*;
import test.*;
import common.*;
import storage.*;
import naming.*;

/** Base class for storage server tests.

    <p>
    This class takes care of creating a temporary directory for the storage
    server to serve and starting a test naming server on startup, and removing
    the directory and stopping the servers on exit.
 */
abstract class StorageTest extends Test
{
    /** Temporary directory served by the storage server. */
    protected TemporaryDirectory    directory = null;
    /** Storage server being tested. */
    private StorageServer           server = null;
    /** Stub for the storage server client service. */
    protected Storage               client_stub = null;
    /** Stub for the storage server command service. */
    protected Command               command_stub = null;
    /** Testing naming server. */
    protected TestNamingServer      naming_server = null;
    /** Naming server registration service. */
    protected Registration          naming_stub = null;

    /** Files to be created in the storage server temporary directory. */
    private String[][]              test_files;
    /** Files that the naming server should command the storage server to delete
        after registration. */
    private Path[]                  delete_files;

    /** Creates a <code>StorageTest</code> object.

        @param test_files Files to be created in the storage server root
                          directory.
        @param delete_files Files the naming server is to command the storage
                            server to delete.
     */
    protected StorageTest(String[][] test_files, Path[] delete_files)
    {
        this.test_files = test_files;
        this.delete_files = delete_files;
    }

    /** Initializes the temporary directory and servers to be used for the test.

        @throws TestFailed If the test objects cannot be initialized.
     */
    protected void initialize() throws TestFailed
    {
        // Create the temporary directory and populate it with files.
        try
        {
            directory = new TemporaryDirectory();
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create temporary directory", t);
        }

        try
        {
            if(test_files != null)
            {
                for(String[] path : test_files)
                    directory.add(path);
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to add file to temporary " +
                                 "directory", t);
        }

        // Assemble the list of expected files.
        Path[]      expect_files = null;

        if(test_files != null)
        {
            expect_files = new Path[test_files.length];

            for(int index = 0; index < test_files.length; ++index)
            {
                Path    current_path = new Path();
                for(String component : test_files[index])
                    current_path = new Path(current_path, component);

                expect_files[index] = current_path;
            }
        }

        // Create and start the test naming server.
        try
        {
            naming_server =
                new TestNamingServer(this);

            naming_server.expectFiles(expect_files);
            naming_server.deleteFiles(delete_files);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create test naming server", t);
        }

        naming_server.start();
        naming_stub = naming_server.stub();

        // Create the storage server.
        try
        {
            server = new StorageServer(directory.root());
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create storage server", t);
        }

        // Start the storage server.
        try
        {
            server.start("127.0.0.1", naming_stub);
        }
        catch(Throwable t)
        {
            throw new TestFailed("cannot start storage server", t);
        }

        // Retrieve the storage server stubs.
        client_stub = naming_server.clientInterface();
        command_stub = naming_server.commandInterface();
    }

    /** Stops the testing servers and removes the temporary directory. */
    @Override
    protected void clean()
    {
        if(server != null)
        {
            server.stop();
            server = null;
        }

        if(naming_server != null)
        {
            naming_server.stop();
            naming_server = null;
        }

        if(directory != null)
        {
            directory.remove();
            directory = null;
        }
    }
}
