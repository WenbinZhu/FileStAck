package naming;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.HashMap;

import rmi.*;
import common.*;
import storage.*;

class ServerStubs
{
    public Storage storageStub;
    public Command commandStub;

    public ServerStubs(Storage storageStub, Command commandStub)
    {
        this.storageStub = storageStub;
        this.commandStub = commandStub;
    }
}

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
    private HashMap<PathNode, ServerStubs> nodeStubMap;
    private Skeleton<Registration> regSkeleton;
    private Skeleton<Service> serSkeleton;
    private volatile boolean canStart = true;

    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
        this.root = new PathNode(false, new Path());
        this.nodeStubMap = new HashMap<>();
        this.regSkeleton = new Skeleton<Registration>(Registration.class, this,
                new InetSocketAddress(NamingStubs.REGISTRATION_PORT));
        this.serSkeleton = new Skeleton<Service>(Service.class, this,
                new InetSocketAddress(NamingStubs.SERVICE_PORT));
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
        }
        catch (Exception e) {
            stopped(e);
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
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
