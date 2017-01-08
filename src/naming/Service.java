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
    /** Locks a file or directory for either shared or exclusive access.

        <p>
        An object locked for <em>exclusive</em> access cannot be locked by any
        other user until the exclusive lock is released. An object should be
        locked for exclusive access when operations performed by the user will
        change the object's state.

        <p>
        An object locked for <em>shared</em> access can be locked by other users
        for shared access at the same time, but cannot be simultaneously locked
        by users requesting exclusive access. This kind of lock should be
        obtained when the object's state will be consulted, but not modified,
        and to prevent the object from being modified by another user.

        <p>
        Wherever there is a requirement that an object be locked for shared
        access, it is acceptable to lock the object for exclusive access
        instead: exclusive access is more "safe" than shared access. However, it
        is best to avoid this unless absolutely necessary, to permit as many
        users simultaneous access to the object as safely possible.

        <p>
        Locking a file for shared access is considered by the naming server to
        be a read request, and may cause the file to be replicated. Locking a
        file for exclusive access is considered to be a write request, and
        causes all copies of the file but one to be deleted. This latter process
        is called invalidation. The naming server must treat lock actions as
        read or write requests because it cannot monitor the true read and write
        requests - those go to the storage servers.

        <p>
        When any object is locked for either kind of access, all objects along
        the path up to, but not including, the object itself, are locked for
        shared access to prevent their modification or deletion by other users.
        For example, if one user locks <code>/etc/scripts/startup.sh</code> for
        exclusive access in order to write to it, then <code>/</code>,
        <code>/etc</code>, <code>/etc/scripts</code> will all be locked for
        shared access to prevent other users from, say, deleting them. This
        locking is done in order from root to leaf - a different locking order
        could result in deadlocks.

        <p>
        An object can be considered to be <em>effectively locked</em> for
        exclusive access if one of the directories on the path to it is already
        locked for exclusive access: this is because no user will be able to
        obtain any kind of lock on the object until the exclusive lock on the
        directory is released. This is a direct consequence of the locking order
        described in the previous paragraph. As a result, if a directory is
        locked for exclusive access, the entire subtree under that directory can
        also be considered to be locked for exclusive access. If a client takes
        advantage of this fact to lock a directory and then perform several
        accesses to the files under it, it should take care not to access files
        for writing: this may cause the naming server to miss true write
        requests to those files, and cause the naming server to fail to request
        that stale copies of the file be invalidated.

        <p>
        A minimal amount of fairness is guaranteed with locking: users are
        served in first-come first-serve order, with a slight modification:
        users requesting shared access are granted the lock simultaneously. As a
        consequence of the lock service order, if at least one exclusive user
        is already waiting for the lock, subsequent users requesting shared
        access must wait until that user has released the lock - even if the
        lock is currently taken for shared access. For example, suppose users
        <code>A</code> and <code>B</code> both currently hold the lock with
        shared access. User <code>C</code> arrives and requests exclusive
        access. User <code>C</code> is then placed in a queue. If another user,
        <code>D</code>, arrives and requests shared access, he is not permitted
        to take the lock immediately, even though it is currently taken by
        <code>A</code> and <code>B</code> for shared access. User <code>D</code>
        must wait until <code>C</code> is done with the lock.

        @param path The file or directory to be locked.
        @param exclusive If <code>true</code>, the object is to be locked for
                         exclusive access. Otherwise, it is to be locked for
                         shared access.
        @throws FileNotFoundException If the object specified by
                                      <code>path</code> cannot be found.
        @throws IllegalStateException If the object is a file, the file is
                                      being locked for write access, and a stale
                                      copy cannot be deleted from a storage
                                      server for any reason, or if the naming
                                      server has shut down and the lock attempt
                                      has been interrupted.
        @throws RMIException If the call cannot be completed due to a network
                             error. This includes server shutdown while a client
                             is waiting to obtain the lock.
     */
    public void lock(Path path, boolean exclusive)
        throws RMIException, FileNotFoundException;

    /** Unlocks a file or directory.

        @param path The file or directory to be unlocked.
        @param exclusive Must be <code>true</code> if the object was locked for
                         exclusive access, and <code>false</code> if it was
                         locked for shared access.
        @throws IllegalArgumentException If the object specified by
                                         <code>path</code> cannot be found. This
                                         is a client programming error, as the
                                         path must have previously been locked,
                                         and cannot be removed while it is
                                         locked.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public void unlock(Path path, boolean exclusive) throws RMIException;

    /** Determines whether a path refers to a directory.

        <p>
        The parent directory should be locked for shared access before this
        operation is performed. This is to prevent the object in question from
        being deleted or re-created while this call is in progress.

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

        <p>
        The directory should be locked for shared access before this operation
        is performed, because this operation reads the directory's child list.

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

        <p>
        The parent directory should be locked for exclusive access before this
        operation is performed.

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

        <p>
        The parent directory should be locked for exclusive access before this
        operation is performed.

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

        <p>
        The parent directory should be locked for exclusive access before this
        operation is performed.

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

        <p>
        If the client intends to perform calls only to <code>read</code> or
        <code>size</code> after obtaining the storage server stub, it should
        lock the file for shared access before making this call. If it intends
        to perform calls to <code>write</code>, it should lock the file for
        exclusive access.

        @param file Path to the file.
        @return A stub for communicating with the storage server.
        @throws FileNotFoundException If the file does not exist.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public Storage getStorage(Path file)
        throws RMIException, FileNotFoundException;
}
