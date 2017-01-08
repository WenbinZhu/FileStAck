package naming;

import common.*;
import storage.*;
import rmi.RMIException;

/** Naming server registration interface.

    <p>
    This interface is used once, on startup, by each storage server.
 */
public interface Registration
{
    /** Registers a storage server with the naming server.

        <p>
        The storage server notifies the naming server of the files that it is
        hosting. Note that the storage server does not notify the naming server
        of any directories. The naming server attempts to add as many of these
        files as possible to its directory tree. The naming server then replies
        to the storage server with a subset of these files that the storage
        server must delete from its local storage.

        <p>
        After the storage server has deleted the files as commanded, it must
        prune its directory tree by removing all directories under which no
        files can be found. This includes, for example, directories which
        contain only empty directories.

        @param client_stub Storage server client service stub. This will be
                           given to clients when operations need to be performed
                           on a file on the storage server.
        @param command_stub Storage server command service stub. This will be
                            used by the naming server to issue commands that
                            modify the directory tree on the storage server.
        @param files The list of files stored on the storage server. This list
                     is merged with the directory tree already present on the
                     naming server. Duplicate filenames are dropped.
        @return A list of duplicate files to delete on the local storage of the
                registering storage server.
        @throws IllegalStateException If the storage server is already
                                      registered.
        @throws NullPointerException If any of the arguments is
                                     <code>null</code>.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files) throws RMIException;
}
