package storage;

import java.io.*;
import java.net.*;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
    private File root;
    private Skeleton<Storage> storageSkeleton;
    private Skeleton<Command> commandSkeleton;
    private volatile boolean canStart = true;

    /** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root)
    {
        if (root == null)
            throw new NullPointerException("Parameter root is null");

        this.root = root;
        this.storageSkeleton = new Skeleton<Storage>(Storage.class, this);
        this.commandSkeleton = new Skeleton<Command>(Command.class, this);
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        if (!canStart)
            throw new RMIException("Storage server has stopped, cannot be restarted");
        if (!root.exists() || root.isFile())
            throw new FileNotFoundException("Root either does not exists or is a file");

        storageSkeleton.start();
        commandSkeleton.start();

        // Check if hostname is valid, may throw UnknownHostException
        InetAddress address = InetAddress.getByName(hostname);

        Storage storageStub = Stub.create(Storage.class, storageSkeleton, hostname);
        Command commandStub = Stub.create(Command.class, commandSkeleton, hostname);

        Path[] duplicates = naming_server.register(storageStub, commandStub, Path.list(root));

        for (Path dupPath : duplicates) {
            dupPath.toFile(root).delete();
            prune(dupPath.toFile(root).getParentFile());
        }
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        canStart = false;
        storageSkeleton.stop();
        commandSkeleton.stop();
        stopped(null);
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        throw new UnsupportedOperationException("not implemented");
    }


    /** Prune empty directories bottom-up
     */
    private synchronized void prune(File parent)
    {
        if (parent == root)
            return;

        if (parent.list().length == 0)
            parent.delete();
        else
            return;

        prune(parent.getParentFile());
    }
}
