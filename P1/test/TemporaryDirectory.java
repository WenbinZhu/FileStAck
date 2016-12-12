package test;

import java.io.*;
import java.lang.*;
import java.lang.ref.*;

/** Temporary directories for testing.

    <p>
    Temporary directories are created under the directory given by the system
    property <code>java.io.tmpdir</code>. Each has the name
    <code>dist-systems-n</code>, where <code>n</code> is a number between
    <code>0</code> and <code>1023</code>.

    <p>
    Temporary directories should be removed manually by calling
    <code>remove</code>. If, however, this function is not called, they will be
    removed automatically when the virtual machine exits.
 */
public class TemporaryDirectory
{
    /** <code>File</code> object representing the directory. */
    private final File          directory;
    /** Flag used to indicate that the directory has already been removed. This
        is used to allow multiple calls to <code>remove</code>. Calls after the
        first one should not remove the directory - because the directory may
        have been since re-created for another purpose. */
    private boolean             removed;

    /** Upper bound on temporary directory name suffixes. */
    private static final int    bound = 1024;

    /** Creates a temporary directory.

        @throws FileNotFoundException If a directory cannot be created. This can
                                      occur due to permissions problems, or due
                                      to the exhaustion of temporary directory
                                      names.
     */
    public TemporaryDirectory() throws FileNotFoundException
    {
        String      temp_root_name = System.getProperty("java.io.tmpdir");
        File        temp_root = new File(temp_root_name);

        for(int index = 0; index < bound; ++index)
        {
            String  name = "dist-systems-" + index;
            File    attempt = new File(temp_root, name);

            if(attempt.mkdir())
            {
                directory = attempt;
                removed = false;

                // Make sure the directory is removed on virtual machine exit.
                Thread  hook = new Thread(new CleanupShutdownHook(this));
                Runtime.getRuntime().addShutdownHook(hook);

                return;
            }
        }

        throw new FileNotFoundException("unable to create temporary directory");
    }

    /** Recursively deletes a directory.

        @param file The directory to be deleted.
        @return <code>true</code> if the directory is successfully deleted, and
                false otherwise.
     */
    private boolean deleteRecursive(File file)
    {
        if(file.isDirectory())
        {
            for(String child : file.list())
            {
                if(!deleteRecursive(new File(file, child)))
                    return false;
            }
        }

        return file.delete();
    }

    /** Removes a temporary directory.

        <p>
        This method may be called multiple times. If the directory is
        successfully removed, subsequent calls will do nothing. This ensures
        that subsequent calls to <code>remove</code> will not delete a new
        temporary directory created for another purpose.
     */
    public synchronized void remove()
    {
        if(!removed)
        {
            removed = deleteRecursive(directory);
        }
    }

    /** Retrieves the <code>File</code> object representing the temporary
        directory.

        @return The <code>File</code> object.
     */
    public File root()
    {
        return directory;
    }

    /** Recursively adds a file to the temporary directory.

        @param path Path to the file, represented as an array of path
                    components. The subdirectory in which the file is located is
                    created if it does not exist.
        @return A <code>File</code> object representing the new file.
        @throws IllegalArgumentException If <code>path</code> represents the
                                         temporary directory itself.
        @throws IOException If the file cannot be created.
     */
    private File addPrivate(String[] path) throws IOException
    {
        if(path.length < 1)
            throw new IllegalArgumentException("path is the root directory");

        // Find or create the directory in which the file will be located.
        File    current_directory = directory;

        for(int index = 0; index < path.length - 1; ++index)
        {
            current_directory = new File(current_directory, path[index]);

            current_directory.mkdir();
            if(!current_directory.isDirectory())
            {
                throw new IOException("path component " + path[index] + " is " +
                                      "not a directory or cannot be created");
            }
        }

        // Create the file.
        File    file = new File(current_directory, path[path.length - 1]);

        if(file.createNewFile())
            return file;
        else
        {
            throw new IOException("unable to create file " +
                                  path[path.length - 1]);
        }
    }

    /** Adds a file to the temporary directory.

        @param path The path to the file.
        @throws IllegalArgumentException If <code>path</code> represents the
                                         temporary directory itself.
        @throws IOException If the file cannot be created.
     */
    public void add(String[] path) throws IOException
    {
        addPrivate(path);
    }

    /** Adds a file with the given contents to the temporary directory.

        @param path The path to the file.
        @param contents The contents of the file.
        @throws IllegalArgumentException If <code>path</code> represents the
                                         temporary directory itself.
        @throws IOException If the file cannot be created or the contents cannot
                            be written.
     */
    public void add(String[] path, String contents) throws IOException
    {
        File            file = addPrivate(path);
        PrintWriter     writer;

        try
        {
            writer = new PrintWriter(file);
        }
        catch(FileNotFoundException e)
        {
            throw new IOException("did not create writeable file " +
                                  path[path.length - 1], e);
        }

        writer.print(contents);
        writer.close();
    }

    /** Attempts to ensure that the directory is deleted when the object is
        garbage-collected. */
    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            remove();
        }
        finally
        {
            super.finalize();
        }
    }

    /** Cleanup task.

        <p>
        This class implements a task which will be run upon JVM shutdown. It
        contains a weak reference to the temporary directory object. This
        permits the temporary directory to be cleaned by the garbage collector
        as if the cleanup task did not exist. If the temporary directory object
        survives until JVM shutown, this task ensures that the temporary
        directory is removed.
     */
    private static class CleanupShutdownHook implements Runnable
    {
        /** A weak reference to the TemporaryDirectory object. */
        private final WeakReference<TemporaryDirectory> directory;

        /** Creates and initializes the cleanup task.

            @param directory The temporary directory to be removed on JVM
                             shutdown, if not previously removed by a call to
                             <code>remove</code> or <code>finalize</code>.
         */
        CleanupShutdownHook(TemporaryDirectory directory)
        {
            this.directory = new WeakReference<TemporaryDirectory>(directory);
        }

        /** Attempts to ensure that the directory is deleted when the virtual
            machine exits. */
        @Override
        public void run()
        {
            try
            {
                directory.get().remove();
            }
            catch(Throwable t) { }
        }
    }
}
