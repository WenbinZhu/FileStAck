package conformance.storage;

import test.*;
import common.*;
import storage.*;
import java.io.*;
import java.util.*;

/** Tests storage server <code>copy</code> method.

    <p>
    The test starts two storage servers and a test naming server. It then checks
    properties of the <code>copy</code> method.

    <p>
    Properties checked are:
    <ul>
    <li><code>copy</code> rejects <code>null</code> for any of its
        arguments.</li>
    <li><code>copy</code> rejects source directories and non-existent source
        files.</li>
    <li><code>copy</code> correctly creates new destination files and their
        parent directories.</li>
    <li><code>copy</code> correctly replaces existing destination files.</li>
    </ul>
 */
public class ReplicationTest extends StorageTest
{
    /** Test notice. */
    public static final String  notice =
        "checking storage server replication process";
    /** Prerequisites. */
    public static final Class[] prerequisites = new Class[] {AccessTest.class};

    /** Second temporary directory for the second (source) storage server. */
    private TemporaryDirectory  second_directory = null;
    /** Second (source) storage server. */
    private StorageServer       second_server = null;
    /** Client service interface for the second storage server. */
    private Storage             second_stub = null;

    /** File to be copied. This file already exists on the destination
        server. */
    private final Path          replace_path = new Path("/file4");
    /** File to be copied. This file is new on the destination server. */
    private final Path          create_path = new Path("/replicate/file5");

    /** Creates the <code>ReplicationTest</code> object. */
    public ReplicationTest()
    {
        super(new String[][] {new String[] {"file4"}}, null);
    }

    /** Tests the copy method.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testBadValues();
        testCreation();
        testReplacement();
    }

    /** Checks that the <code>copy</code> method correctly creates new files.

        @throws TestFailed If the test fails.
     */
    private void testCreation() throws TestFailed
    {
        // Data stored in the file to be copied.
        byte[]  data = "data".getBytes();

        // Attempt to write the data to the file on the source server.
        try
        {
            second_stub.write(create_path, 0, data);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to write data to file to be copied",
                                 t);
        }

        // Copy the file to the destination server, creating its parent
        // directory.
        try
        {
            if(!command_stub.copy(create_path, second_stub))
            {
                throw new TestFailed("unable to create new file by " +
                                     "replication: copy returned false");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create new file by replication", t);
        }

        // Check that the file copy has the correct size and contents.
        long    resulting_size;
        byte[]  resulting_data;

        try
        {
            resulting_size = client_stub.size(create_path);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve size of created file", t);
        }

        if(resulting_size != (long)data.length)
            throw new TestFailed("created file has incorrect size");

        try
        {
            resulting_data =
            client_stub.read(create_path, 0, (int)resulting_size);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve contents of created file",
                                 t);
        }

        if(!Arrays.equals(resulting_data, data))
            throw new TestFailed("created file has incorrect contents");
    }

    /** Checks that the <code>copy</code> method correctly replaces existing
        files.

        @throws TestFailed If the test fails.
     */
    private void testReplacement() throws TestFailed
    {
        // Write some data to the file to be replaced; write less data to the
        // file with which it will be replaced. Then, order the file to be
        // replaced. It should be truncated to the size of the copied file.
        byte[]  old_data = "old data".getBytes();
        byte[]  new_data = "data".getBytes();

        try
        {
            client_stub.write(replace_path, 0, old_data);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to write data to file to be " +
                                 "replaced on first storage server", t);
        }

        try
        {
            second_stub.write(replace_path, 0, new_data);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to write data to replacing file on " +
                                 "second storage server", t);
        }

        // Order the file on the first server to be replaced.
        try
        {
            if(!command_stub.copy(replace_path, second_stub))
            {
                throw new TestFailed("unable to replace file by " +
                                     "replication: copy returned false");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("unable to replace file by replication", t);
        }

        // Check the size and contents of the file after it is replaced.
        long    resulting_size;
        byte[]  resulting_data;

        try
        {
            resulting_size = client_stub.size(replace_path);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve size of replaced file", t);
        }

        if(resulting_size != (long)new_data.length)
            throw new TestFailed("replaced file has incorrect size");

        try
        {
            resulting_data =
                client_stub.read(replace_path, 0, (int)resulting_size);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve contents of replaced file",
                                 t);
        }

        if(!Arrays.equals(resulting_data, new_data))
            throw new TestFailed("replaced file has incorrect contents");
    }

    /** Tests that the <code>copy</code> method rejects bad arguments such as
        <code>null</code>.

        @throws TestFailed If any of the tests fail.
     */
    private void testBadValues() throws TestFailed
    {
        // Attempt to pass null arguments to copy.
        try
        {
            command_stub.copy(replace_path, null);
            throw new TestFailed("copy accepted null for peer storage server " +
                                 "interface");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("copy threw unexpected exception when given " +
                                 "null for peer storage server interface", t);
        }

        try
        {
            command_stub.copy(null, second_stub);
            throw new TestFailed("copy accepted null for path");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("copy threw unexpected exception when given " +
                                 "null for path", t);
        }

        // Attempt to copy a non-existent file.
        try
        {
            command_stub.copy(new Path("/absent-file"), second_stub);
            throw new TestFailed("copy succeeded when given missing file");
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("copy threw unexpected exception when given " +
                                 "missing file", t);
        }

        // Attempt to copy a directory.
        try
        {
            command_stub.copy(new Path("/replicate"), second_stub);
            throw new TestFailed("copy succeeded when given directory");
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("copy threw unexpected exception when given " +
                                 "directory", t);
        }
    }

    /** Initializes the test.

        <p>
        This implementation calls the superclass implementation, and
        additionally creates a second temporary directory and starts a second
        storage server.

        @throws TestFailed If initialization fails.
     */
    @Override
    protected void initialize() throws TestFailed
    {
        // Initialize the naming server and the first storage server. If that
        // initialization fails, return immediately.
        super.initialize();

        // Create a second temporary directory.
        try
        {
            second_directory = new TemporaryDirectory();
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create second temporary directory",
                                 t);
        }

        // Populate the temporary directory with files.
        try
        {
            second_directory.add(new String[] {"file4"});
            second_directory.add(new String[] {"replicate", "file5"});
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to add file to second temporary " +
                                 "directory" , t);
        }

        // Set the expected files for the registration of the second storage
        // server.
        naming_server.expectFiles(new Path[] {replace_path, create_path});
        naming_server.deleteFiles(null);

        // Create the second storage server.
        try
        {
            second_server =
                new StorageServer(second_directory.root());
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create second storage server", t);
        }

        // Start the second storage server.
        try
        {
            second_server.start("127.0.0.1", naming_stub);
        }
        catch(Throwable t)
        {
            throw new TestFailed("cannot start second storage server", t);
        }

        // Retrieve storage server client interface stub.
        second_stub = naming_server.clientInterface();
    }

    /** Stops all servers and removes all temporary directories.

        <p>
        This implementation stops the second storage server and removes its
        temporary directory, in addition to calling the superclass
        implementation.
     */
    @Override
    protected void clean()
    {
        super.clean();

        if(second_server != null)
        {
            second_server.stop();
            second_server = null;
        }

        if(second_directory != null)
        {
            second_directory.remove();
            second_directory = null;
        }
    }
}
