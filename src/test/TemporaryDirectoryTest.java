package test;

import java.io.*;

/** Checks <code>TemporaryDirectory</code>.

    <p>
    Tests performed are:
    <ul>
    <li>Temporary directories can be created.</li>
    <li>Temporary directories can be deleted.</li>
    <li>Distinct temporary directories are created.</li>
    <li>Files can be added to a temporary directory.</li>
    </ul>
 */
public class TemporaryDirectoryTest extends Test
{
    /** Test notice. */
    public final static String  notice =
        "checking temporary directory creation and deletion";

    /** Temporary directory object. */
    private TemporaryDirectory  directory = null;
    /** Second temporary directory object. */
    private TemporaryDirectory  second_directory = null;

    /** Creates the two temporary directories. */
    @Override
    protected void initialize() throws TestFailed
    {
        try
        {
            directory = new TemporaryDirectory();
            second_directory = new TemporaryDirectory();
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create temporary directory", t);
        }
    }

    /** Performs the tests. */
    @Override
    protected void perform() throws TestFailed
    {
        // Get the File objects corresponding to both directories.
        File    root = directory.root();
        File    second_root = second_directory.root();

        // Make sure the two directories are not the same.
        if(root.equals(second_root))
        {
            throw new TestFailed("temporary directories created with the " +
                                 "same path");
        }

        // Make sure both directories are indeed directories and exist.
        if(!root.isDirectory() || !second_root.isDirectory())
        {
            throw new TestFailed("temporary directory does not exist or is " +
                                 "not a directory");
        }

        // Delete the second directory - it is no longer needed.
        second_directory.remove();

        // There is a potential race condition here - the second directory could
        // be re-created before this statement runs. However, this is not the
        // common case when the library is being tested.
        if(second_root.exists())
            throw new TestFailed("temporary directory not removed");

        // Try to add a file to the first directory.
        try
        {
            directory.add(new String[] {"subdir", "file.txt"}, "contents");
        }
        catch(Exception e)
        {
            throw new TestFailed("unable to create file in temporary directory",
                                 e);
        }

        // Delete the first directory. This ensures that recursive deletion
        // works.
        directory.remove();

        if(root.exists())
            throw new TestFailed("temporary directory not removed");
    }

    /** Makes an effort to remove both temporary directories. */
    @Override
    protected void clean()
    {
        if(directory != null)
            directory.remove();

        if(second_directory != null)
            second_directory.remove();
    }
}
