package conformance.storage;

import test.*;
import common.*;
import java.io.*;

/** Tests storage server directory manipulation methods.

    <p>
    This test starts a storage server and a test naming server. It then checks
    properties of the <code>create</code> and <code>delete</code> methods.

    <p>
    Properties checked are:
    <ul>
    <li><code>create</code> and <code>delete</code> do not accept
        <code>null</code> as an argument.</li>
    <li><code>create</code> and <code>delete</code> do not accept the root
        directory as an argument.</li>
    <li><code>create</code> fails when given a directory or an existing
        file as argument.</li>
    <li><code>create</code> creates the parent directory of a new file if the
        parent directory does not yet exist.</li>
    <li><code>create</code> creates files in the root directory. This is
        important to check if <code>mkdirs</code> is being used to create the
        parent directory of the file.</li>
    <li><code>create</code> creates files that are accessible through both
        <code>read</code> and <code>write</code>.</li>
    <li><code>delete</code> fails for non-existent files.</li>
    <li><code>delete</code> deletes regular files and recursively deletes
        directories.</li>
    </ul>
 */
public class DirectoryTest extends StorageTest
{
    /** Test notice. */
    public static final String  notice =
        "checking storage server file manipulation methods (create, delete)";
    /** Prerequisites. */
    public static final Class[] prerequisites = new Class[] {AccessTest.class};

    /** File to be created in the root directory. */
    private final Path  root_create_path = new Path("/file4");
    /** File to be created in a new directory. */
    private final Path  create_path = new Path("/dir/dir/file");

    /** Creates the <code>DirectoryTest</code> object. */
    public DirectoryTest()
    {
        super(new String[][] {new String[] {"subdirectory", "file1"},
                              new String[] {"subdirectory", "file2"},
                              new String[] {"file3"},
                              new String[] {"subdirectory", "subdirectory2",
                                            "file1"}},
              null);
    }

    /** Tests the server directory manipulation methods.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testCreate();
        testDelete();
    }

    /** Tests the create method

        @throws TestFailed If any of the tests fail.
     */
    private void testCreate() throws TestFailed
    {
        // Attempt to call create with null as argument.
        try
        {
            command_stub.create(null);
            throw new TestFailed("create accepted null argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("create threw unexpected exception when " +
                                 "given null as argument", t);
        }

        // Attempt to call create with root directory as argument.
        try
        {
            if(command_stub.create(new Path("/")))
            {
                throw new TestFailed("create accepted root directory as " +
                                     "argument");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("create threw unexpected exception when " +
                                 "given root directory as argument", t);
        }

        // Attempt to call create with existing file as argument.
        try
        {
            if(command_stub.create(new Path("/file3")))
            {
                throw new TestFailed("create succeeded when given existing " +
                                     "file as argument");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("create threw unexpected exception when " +
                                 "given existing file as argument", t);
        }

        // Create a new file and all the directories in its parent path.
        try
        {
            if(!command_stub.create(create_path))
                throw new TestFailed("unable to create new file");
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("create threw unexpected exception when " +
                                 "creating new file", t);
        }

        // Check the local directory for the presence of the new file.
        File    local_file = new File(directory.root(), "dir");
        if(!local_file.isDirectory())
            throw new TestFailed("created path component is not a directory");

        local_file = new File(local_file, "dir");
        if(!local_file.isDirectory())
            throw new TestFailed("created path component is not a directory");

        local_file = new File(local_file, "file");

        if(!local_file.exists())
        {
            throw new TestFailed("created file is not present in server " +
                                 "storage directory");
        }

        if(local_file.isDirectory())
            throw new TestFailed("created file is a directory");

        // Check that the file is both readable and writable.
        try
        {
            client_stub.read(create_path, 0, 0);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to read from created file", t);
        }

        try
        {
            client_stub.write(create_path, 0, "test".getBytes());
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to write to created file", t);
        }

        // Check for correct usage of mkdirs (if used) by creating a file in the
        // root directory.
        try
        {
            if(!command_stub.create(root_create_path))
            {
                throw new TestFailed("unable to create new file in root " +
                                     "directory");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("create threw unexpected exception when " +
                                 "creating new file in root directory", t);
        }
    }

    /** Tests the delete method.

        @throws TestFailed If any of the tests fail.
     */
    private void testDelete() throws TestFailed
    {
        // Attempt to call delete with null as argument.
        try
        {
            command_stub.delete(null);
            throw new TestFailed("delete accepted null as argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("delete threw unexpected exception when " +
                                 "given null as argument", t);
        }

        // Attempt to call delete with root directory as argument.
        try
        {
            if(command_stub.delete(new Path("/")))
            {
                throw new TestFailed("delete accepted root directory as " +
                                     "argument");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("delete threw unexpected exception when " +
                                 "given root directory as argument", t);
        }

        // Attempt to call delete with non-existent file as argument.
        try
        {
            if(command_stub.delete(new Path("/absent-file")))
            {
                throw new TestFailed("delete succeeded when given " +
                                     "non-existent file as argument");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("delete threw unexpected exception when " +
                                 "given non-existent file as argument", t);
        }

        // Delete a file and check that it is no longer accessible on the local
        // fileystem.
        try
        {
            if(!command_stub.delete(new Path("/file3")))
                throw new TestFailed("unable to delete file");
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("delete threw unexpected exception when " +
                                 "deleting regular file", t);
        }

        File    deleted_file = new File(directory.root(), "file3");

        if(deleted_file.exists())
            throw new TestFailed("deleted file still present in filesystem");

        // Repeat the same test with a directory.
        try
        {
            if(!command_stub.delete(new Path("/subdirectory")))
                throw new TestFailed("unable to delete directory");
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("delete threw unexpected exception when " +
                                 "deleting directory", t);
        }

        File    deleted_directory = new File(directory.root(), "subdirectory");

        if(deleted_directory.exists())
            throw new TestFailed("deleted directory still present in filesystem");
    }
}
