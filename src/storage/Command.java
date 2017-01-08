package storage;

import java.io.*;

import common.*;
import rmi.RMIException;

/** Storage server command interface.

    <p>
    The naming server uses this interface to communicate commands to the storage
    server.

    <p>
    All methods in this interface may raise {@link NullPointerException} if
    passed <code>null</code> for arguments or {@link SecurityException} if the
    security manager on the server does not allow an operation.
 */
public interface Command
{
    /** Creates a file on the storage server.

        @param file Path to the file to be created. The parent directory will be
                    created if it does not exist. This path may not be the root
                    directory.
        @return <code>true</code> if the file is created; <code>false</code>
                if it cannot be created.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public boolean create(Path file) throws RMIException;

    /** Deletes a file or directory on the storage server.

        <p>
        If the file is a directory and cannot be deleted, some, all, or none of
        its contents may be deleted by this operation.

        @param path Path to the file or directory to be deleted. The root
                    directory cannot be deleted.
        @return <code>true</code> if the file or directory is deleted;
                <code>false</code> otherwise.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public boolean delete(Path path) throws RMIException;
}
