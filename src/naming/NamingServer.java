package naming;

import java.io.*;
import java.util.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{
    private PathNode root;
    private Skeleton<Registration> regSkeleton;
    private Skeleton<Service> serSkeleton;
    private ArrayList<ServerStubs> registeredStubs;
    private ConcurrentHashMap<Path, ReadWriteLock> lockTable;
    private volatile boolean canStart;

    private static final double ALPHA = 0.2;
    private static final int MULTIPLE = 20;
    private static final int REPLICA_UPPER_BOUND = 20;

    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
        this.root = new PathNode(new Path(), null);

        this.regSkeleton = new Skeleton<Registration>(Registration.class, this,
                new InetSocketAddress(NamingStubs.REGISTRATION_PORT));
        this.serSkeleton = new Skeleton<Service>(Service.class, this,
                new InetSocketAddress(NamingStubs.SERVICE_PORT));

        this.registeredStubs = new ArrayList<>();
        this.lockTable = new ConcurrentHashMap<>();
        this.lockTable.put(root.getPath(), new ReadWriteLock());
        this.canStart = true;
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        if (!canStart)
            throw new RMIException("Skeleton failed to start. Do not start naming server again");

        try {
            regSkeleton.start();
            serSkeleton.start();
        }
        catch (RMIException re) {
            canStart = false;
            throw new RMIException("Unable to start registration/service skeleton", re);
        }
    }

    /** Stops the naming server.

        <p>
        This method waits for both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        try {
            regSkeleton.stop();
            serSkeleton.stop();
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

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Service.java.
    @Override
    public void lock(Path path, boolean exclusive)
        throws RMIException, FileNotFoundException
    {
        if (path == null)
            throw new NullPointerException("Path parameter is null");

        // Test if the path is valid
        PathNode node = root.getNodeByPath(path);

        PathNode curNode = root;
        Path curPath = curNode.getPath();

        // Lock root node
        try {
            if (path.isRoot() && exclusive) {
                lockTable.get(root.getPath()).lockExclusive();
            }
            else {
                lockTable.get(root.getPath()).lockShared();
            }
        }
        catch (InterruptedException ie) {
            return;
        }

        // Lock along the path
        for (String component : path) {
            if (curNode.getChildren().containsKey(component)) {
                curNode = curNode.getChildren().get(component);
                curPath = curNode.getPath();

                if (!lockTable.containsKey(curPath))
                    lockTable.put(curPath, new ReadWriteLock());

                try {
                    if (exclusive && component.equals(path.last())) {
                        lockTable.get(curPath).lockExclusive();

                        if (curNode.isFile()) {
                            curNode.resetAccessTime();
                            deleteReplicas(curNode);
                        }
                    }
                    else {
                        lockTable.get(curPath).lockShared();

                        if (curNode.isFile() && curNode.incAccessTime(MULTIPLE))
                            replicate(curNode);
                    }
                }
                catch (InterruptedException ie) {
                    throw new IllegalStateException("Naming server has shut down, " +
                                                    "lock attempt interrupted");
                }
            }
            else {
                // Unlock all previously locked paths when locking fails
                unlock(curPath, false);
                throw new FileNotFoundException("Locking fails at some node");
            }
        }
    }

    @Override
    public void unlock(Path path, boolean exclusive) throws RMIException
    {
        if (path == null)
            throw new NullPointerException("Path parameter is null");

        // Test if the path is valid
        try {
            root.getNodeByPath(path);
        }
        catch (FileNotFoundException fne) {
            throw new IllegalArgumentException("Unable to find path for unlocking");
        }

        String last = path.isRoot() ? null : path.last();
        Path curPath = path;

        // Unlock along the path
        while (!curPath.isRoot()) {
            if (curPath.last().equals(last) && exclusive) {
                if (lockTable.containsKey(curPath))
                    lockTable.get(curPath).unlockExclusive();
                else
                    throw new IllegalArgumentException("Unable to find path for unlocking parents");
            }
            else {
                if (lockTable.containsKey(curPath))
                    lockTable.get(curPath).unlockShared();
                else
                    throw new IllegalArgumentException("Unable to find path for unlocking parents");
            }

            curPath = curPath.parent();
        }

        // Unlock root node in the last
        if (path.isRoot() && exclusive) {
            lockTable.get(root.getPath()).unlockExclusive();
        }
        else {
            lockTable.get(root.getPath()).unlockShared();
        }
    }

    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        if (path == null)
            throw new NullPointerException("Path parameter is null");

        return !root.getNodeByPath(path).isFile();
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        if (directory == null)
            throw new NullPointerException("Path parameter is null");

        PathNode pathNode = root.getNodeByPath(directory);
        ArrayList<String> contents = new ArrayList<>();

        if (pathNode.isFile())
            throw new FileNotFoundException("Path parameter does not refer to a directory");

        for (String component : pathNode.getChildren().keySet()) {
            contents.add(component);
        }

        return contents.toArray(new String[0]);
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        if (file == null)
            throw new NullPointerException("Path parameter is null");

        if (file.isRoot())
            return false;

        Random random = new Random();
        String last = file.last();
        PathNode parent = root.getNodeByPath(file.parent());
        ServerStubs selectedStorage = registeredStubs.get(random.nextInt(registeredStubs.size()));

        if (parent.isFile())
            throw new FileNotFoundException("Parent is not a directory");

        if (parent.getChildren().containsKey(last))
            return false;

        // Create file on the selected storage server
        boolean success = selectedStorage.commandStub.create(file);

        // Append new node to the directory tree
        // if storage server has successfully created the file
        if (success)
            parent.addChild(last, new PathNode(file, selectedStorage));

        return success;
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        if (directory == null)
            throw new NullPointerException("Path parameter is null");

        if (directory.isRoot())
            return false;

        String last = directory.last();
        PathNode parent = root.getNodeByPath(directory.parent());

        if (parent.isFile())
            throw new FileNotFoundException("Parent is not a directory");

        if (parent.getChildren().containsKey(last))
            return false;

        parent.addChild(last, new PathNode(directory, null));

        return true;
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        if (file == null)
            throw new NullPointerException("Path parameter is null");

        PathNode pathNode = root.getNodeByPath(file);

        if (!pathNode.isFile())
            throw new FileNotFoundException("Unable to get storage stub, path is a directory");

        return pathNode.getStubs().storageStub;
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        if (client_stub == null || command_stub == null || files == null)
            throw new NullPointerException("Parameters of register function has null value");

        PathNode curNode;
        ArrayList<Path> duplicates = new ArrayList<>();
        ServerStubs stubs = new ServerStubs(client_stub, command_stub);

        if (registeredStubs.contains(stubs))
            throw new IllegalStateException("Storage server has already registered");

        registeredStubs.add(stubs);

        for (Path path : files) {
            // System.out.println(path + " " + duplicates.size());
            curNode = root;

            for (String component : path) {
                PathNode childNode = curNode.getChildren().get(component);

                if (childNode != null && childNode.isFile()) {
                    duplicates.add(path);
                }
                else if (childNode != null && !childNode.isFile()) {
                    curNode = childNode;
                }
                else {
                    curNode.addChild(component, new PathNode(new Path(curNode.getPath(), component), null));
                    curNode = curNode.getChildren().get(component);
                }
            }
            // Deal with the leaf node
            curNode.setStubs(stubs);
        }

        return duplicates.toArray(new Path[0]);
    }

    // The following methods are private auxiliary methods
    private void replicate(PathNode node) {
        if (node.getReplicaSize() >= REPLICA_UPPER_BOUND)
            return;

        int NumToReplicate = (int) (ALPHA * MULTIPLE);

        // Get all the storage servers that do not contain the replicas of this path
        HashSet<ServerStubs> stubsSet = new HashSet<>(registeredStubs);
        stubsSet.removeAll(node.getReplicaStubs());
        stubsSet.remove(node.getStubs());

        Iterator<ServerStubs> iter = stubsSet.iterator();

        for (int i = 0; i < NumToReplicate && iter.hasNext() && node.getReplicaSize() < REPLICA_UPPER_BOUND;
             i++, iter = stubsSet.iterator()) {

            ServerStubs serverToReplicate = iter.next();

            try {
                if (serverToReplicate.commandStub.copy(node.getPath(), node.getStubs().storageStub)) {
                    // Update the replica server list of this node
                    // and the available server set for the next replication
                    node.addReplicaStub(serverToReplicate);
                    stubsSet.remove(serverToReplicate);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteReplicas(PathNode node) {
        for (ServerStubs ss : node.getReplicaStubs()) {
            try {
                ss.commandStub.delete(node.getPath());
            }
            catch (RMIException rmie) {
                rmie.printStackTrace();
            }

            // Update the replica server list of this node
            node.removeReplicaStub(ss);
        }
    }
}
