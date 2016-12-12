package apps;

import rmi.*;

import naming.NamingServer;

/** Naming server application.

    <p>
    The naming server application does not take any arguments. It starts a
    naming server listening on the default client and registration ports for
    clients and storage servers, respectively.
 */
public class NamingServerApp extends ServerApplication
{
    /** The naming server. */
    private static StoppingNamingServer     server;

    /** Naming server application entry point. */
    public static void main(String[] arguments)
    {
        new NamingServerApp().run(arguments);
    }

    /** Returns <code>"naming"</code>. */
    @Override
    protected String serverType()
    {
        return "naming";
    }

    /** Starts the naming server.

        @param arguments Command line arguments.
        @throws BadUsageException If there are any command line arguments.
        @throws RMIException If the naming server cannot be started.
     */
    @Override
    protected void startServer(String[] arguments)
        throws BadUsageException, RMIException
    {
        if(arguments.length != 0)
            throw new BadUsageException("naming server expects no arguments");

        server = new StoppingNamingServer();
        server.start();
    }

    /** Stops the naming server. */
    @Override
    protected void stopServer()
    {
        server.stop();
    }

    /** Application naming server. */
    private class StoppingNamingServer extends NamingServer
    {
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
        protected void stopped(Throwable cause)
        {
            serverStopped(cause);
        }
    }
}
