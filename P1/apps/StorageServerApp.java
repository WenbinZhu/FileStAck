package apps;

import java.io.*;
import java.net.*;
import java.util.*;

import rmi.*;
import naming.*;

import storage.StorageServer;

/** Storage server application.

    <p>
    The storage server application expects three arguments: in order, the
    local hostname, the hostname of the remote naming server, and the directory
    that the storage server will use as its local storage for files.

    <p>
    The directory can be given as an absolute or a relative path. The current
    contents of the directory will be offered to the naming server during
    registration. Duplicate files in the directory will be deleted, and empty
    directories will be pruned.

    <p>
    For this reason, it is <em>extremely</em> important that the storage server
    not be started in a directory containing important files, as those files may
    be deleted. It is recommended that a special directory be created for each
    storage server, and the storage server only be started in that directory.

    <p>
    The user under which the storage server is run should have full read and
    write access to the directory in which the storage server is started.
 */
public class StorageServerApp extends ServerApplication
{
    /** The storage server itself. */
    private static StoppingStorageServer    server;

    /** Storage server application entry point. */
    public static void main(String[] arguments)
    {
        new StorageServerApp().run(arguments);
    }

    /** Returns <code>"storage"</code>. */
    @Override
    protected String serverType()
    {
        return "storage";
    }

    /** Starts the storage server.

        @param arguments The command line arguments.
        @throws BadUsageException If there are not three arguments on the
                                  command line.
        @throws UnknownHostException If a storage server stub cannot be created
                                     due to an unassigned address.
        @throws FileNotFoundException If the directory in which the storage
                                      server is being started does not exist, or
                                      if the path does not refer to a directory.
        @throws RMIException If the storage server cannot be started or the
                             naming server cannot be contacted for registration.
        @throws IllegalStateException If a storage server with the same hostname
                                      and port number is already registered with
                                      the naming server.
     */
    @Override
    protected void startServer(String[] arguments)
        throws BadUsageException, UnknownHostException, FileNotFoundException,
               RMIException
    {
        // Check the command line arguments.
        if(arguments.length != 3)
        {
            throw new BadUsageException("arguments: hostname naming-server " +
                                        "local-path");
        }

        // Create the storage server object using the absolute version of the
        // given path.
        File            local_root = new File(arguments[2]).getAbsoluteFile();
        server = new StoppingStorageServer(local_root);

        // Start and register the storage server.
        server.start(arguments[0], NamingStubs.registration(arguments[1]));
    }

    /** Stops the storage server. */
    @Override
    protected void stopServer()
    {
        server.stop();
    }

    /** Application storage server. */
    private class StoppingStorageServer extends StorageServer
    {
        /** Creates the storage server. */
        StoppingStorageServer(File root)
        {
            super(root);
        }

        /** Schedules a timeout before attempting to stop the server
            gracefully. */
        @Override
        public void stop()
        {
            startTerminationTimer();
            super.stop();
        }

        /** Calls <code>serverStopped</code>. */
        @Override
        protected synchronized void stopped(Throwable cause)
        {
            serverStopped(cause);
        }
    }
}
