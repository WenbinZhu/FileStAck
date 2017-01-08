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
    private volatile boolean canStart;

    /** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root)
    {
        if (root == null)
            throw new NullPointerException("Parameter root is null");

        if (!root.exists() || !root.isDirectory())
            throw new IllegalArgumentException("Root does not exist or refers to a directory");

        this.root = root;
        this.storageSkeleton = new Skeleton<Storage>(Storage.class, this);
        this.commandSkeleton = new Skeleton<Command>(Command.class, this);
        this.canStart = true;
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
        try {
            storageSkeleton.stop();
            commandSkeleton.stop();
            stopped(null);
        }
        catch (Exception e) {
            stopped(e);
            e.printStackTrace();
        }
        finally {
            canStart = false;
        }
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
        File f = file.toFile(root);

        if (!f.exists() || !f.isFile())
            throw new FileNotFoundException("File cannot be found or the path refers to a directory");

        return f.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        if (file == null)
            throw new NullPointerException();

        File f = file.toFile(root);

        if (!f.exists() || !f.isFile())
            throw new FileNotFoundException("File cannot be found or the path refers to a directory");

        if (!f.canRead())
            throw new IOException("File cannot be read");

        if (offset < 0 || length < 0 || (offset + length) > f.length())
            throw new IndexOutOfBoundsException("Read parameter offset or length out of file's bound");

        byte[] bytes = new byte[length];
        RandomAccessFile reader = new RandomAccessFile(f, "r");
        reader.seek(offset);
        reader.readFully(bytes, 0, length);

        return bytes;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        File f = file.toFile(root);

        if (!f.exists() || !f.isFile())
            throw new FileNotFoundException("File cannot be found or the path refers to a directory");

        if (!f.canWrite())
            throw new IOException("File cannot be written");

        if (offset < 0)
            throw new IndexOutOfBoundsException("Read parameter offset or length out of file's bound");

        RandomAccessFile writer = new RandomAccessFile(f, "rw");
        writer.seek(offset);
        writer.write(data);
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        if (file.isRoot())
            return false;

        File newFile = file.toFile(root);
        File parent = newFile.getParentFile();

        try {
            // if (!parent.mkdirs())
            //     prune(parent);
            parent.mkdirs();
            return newFile.createNewFile();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        if (path.isRoot())
            return false;

        File f = path.toFile(root);

        if (!f.exists())
            return false;

        if (f.isDirectory())
            return deleteDir(f);

        return f.delete();
    }

    /** Delete directory recursively

        <p>
        If a directory is not empty, try to delete all its contents
        and then delete it when it becomes empty
     */
    private boolean deleteDir(File file)
    {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (!deleteDir(f))
                    return false;
            }
        }

        return file.delete();
    }

    /** Prune empty directories bottom-up

        <p>
        If parent directory is empty, delete parent and
        then check the grandparent, until reaches root
     */
    private synchronized void prune(File parent)
    {
        if (!parent.exists() || parent == root || !parent.isDirectory())
            return;

        if (parent.list().length == 0)
            parent.delete();
        else
            return;

        prune(parent.getParentFile());
    }
}
