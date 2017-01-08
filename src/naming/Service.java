package naming;

import java.io.*;
import common.*;
import rmi.RMIException;
import storage.Storage;

/** Naming server client service interface.

    <p>
    This is the interface through which clients access the naming server.

    <p>
    The term <em>object</em> in the documentation below refers to any filesystem
    object: either a file or a directory.
 */
public interface Service
{
    /** Determines whether a path refers to a directory.

        @param path The object to be checked.
        @return <code>true</code> if the object is a directory,
                <code>false</code> if it is a file.
        @throws FileNotFoundException If the object specified by
                                      <code>path</code> cannot be found.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public boolean isDirectory(Path path)
        throws RMIException, FileNotFoundException;

    /** Lists the contents of a directory.

        @param directory The directory to be listed.
        @return An array of the directory entries. The entries are not
                guaranteed to be in any particular order.
        @throws FileNotFoundException If the given path does not refer to a
                                      directory.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public String[] list(Path directory)
        throws RMIException, FileNotFoundException;

    /** Creates the given file, if it does not exist.

        @param file Path at which the file is to be created.
        @return <code>true</code> if the file is created successfully,
                <code>false</code> otherwise. The file is not created if a file
                or directory with the given name already exists.
        @throws FileNotFoundException If the parent directory does not exist.
        @throws IllegalStateException If no storage servers are connected to the
                                      naming server.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException;

    /** Creates the given directory, if it does not exist.

        @param directory Path at which the directory is to be created.
        @return <code>true</code> if the directory is created successfully,
                <code>false</code> otherwise. The directory is not created if
                a file or directory with the given name already exists.
        @throws FileNotFoundException If the parent directory does not exist.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public boolean createDirectory(Path directory)
        throws RMIException, FileNotFoundException;

    /** Deletes a file or directory.

        @param path Path to the file or directory to be deleted.
        @return <code>true</code> if the file or directory is deleted;
                <code>false</code> otherwise. The root directory cannot be
                deleted.
        @throws FileNotFoundException If the object or parent directory does not
                                      exist.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public boolean delete(Path path) throws RMIException, FileNotFoundException;

    /** Returns a stub for the storage server hosting a file.

        @param file Path to the file.
        @return A stub for communicating with the storage server.
        @throws FileNotFoundException If the file does not exist.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public Storage getStorage(Path file)
        throws RMIException, FileNotFoundException;
}
