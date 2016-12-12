package apps;

import java.io.*;
import java.util.*;

/** Base class of server applications.

    <p>
    Server applications are wrappers around the server classes (naming and
    storage) that allow the servers to be run from the console. This class
    provides functionality that is common to both server applications: namely,
    graceful termination.

    <p>
    In each application, the server runs until the end of file is read from
    standard input, or until the server stops on its own due to an exception.
    Server applications also forcefully terminate the JVM if the end of file has
    been read on standard input, but the server object takes too long to stop.

    <p>
    Derived application classes override the <code>serverType</code>,
    <code>startServer</code>, and <code>stopServer</code> methods to deal with
    the particular kind of server used in the application. The <code>stop</code>
    method of each server should be overridden to call
    <code>startTerminationTimer</code>, before calling the superclass
    implementation. The <code>stopped</code> method should be overridden to call
    <code>serverStopped</code>.

    <p>
    Derived classes should create a new object of their own type immediately
    upon entry into <code>main</code>, and call that object's <code>run</code>
    method.
 */
abstract class ServerApplication extends Application
{
    /** Time the server is allotted to stop gracefully, in milliseconds. */
    private static final long   TERMINATION_TIMEOUT = 5000;

    /** Indicates that the server has stopped. */
    private boolean             stopped = false;
    /** If the server stopped due to an exception, gives the exception.
        Otherwise, set to <code>null</code>. */
    private Throwable           stop_cause = null;

    /** Timer used to terminate the JVM forcefully if the server refuses to exit
        gracefully when commanded. */
    private Timer               termination_timeout_timer = new Timer();

    /** Returns a string indicating the kind of server being run in the
        application - either <code>"naming"</code> or <code>"storage"</code>. */
    protected abstract String serverType();

    /** Starts the server.

        @param arguments Command line arguments passed to the application.
        @throws BadUsageException If the command line arguments passed to the
                                  application are malformed. The message string
                                  of the exception object is printed to the
                                  console before the application is terminated.
        @throws Throwable This method is allowed to throw any exception. The
                          exception will be reported on the console, and the
                          application terminated.
     */
    protected abstract void startServer(String[] arguments) throws Throwable;

    /** Stops the server. */
    protected abstract void stopServer();

    /** Runs the application.

        <p>
        This is the non-static replacement for the <code>main</code> method. It
        starts the server, starts a thread to wait for end-of-file for standard
        input, and waits until the server has terminated. Once the server
        terminates, this method prints a message indicating what kind of
        termination has occurred.

        @param arguments The command line arguments passed to <code>main</code>.
     */
    @Override
    protected void run(String[] arguments)
    {
        // Start the server. If the command line arguments are malformed, or the
        // server cannot be started, print a message and terminate the
        // application.
        try
        {
            startServer(arguments);
        }
        catch(BadUsageException e)
        {
            System.err.println(e.getMessage());
            System.exit(EXIT_FAILURE);
        }
        catch(Throwable t)
        {
            System.err.println("unable to start " + serverType() + " server: " +
                               t);
            t.printStackTrace(System.err);
            System.exit(EXIT_FAILURE);
        }

        // Print a message indicating the server has started.
        System.out.println(serverType() + " server started");
        System.out.println("send EOF on standard input (Ctrl+D or Ctrl+Z, " +
                           "Enter) to force server to exit.");

        // Start a thread to monitor for EOF on standard input.
        new Thread(new EOFThread()).start();

        // Wait for the server to stop.
        synchronized(this)
        {
            while(!stopped)
            {
                try
                {
                    wait();
                }
                catch(InterruptedException e) { }
            }
        }

        // If the server stopped with a cause, print the cause. Otherwise, the
        // server stopped normally.
        if(stop_cause == null)
        {
            System.out.println(serverType() + " server stopped normally");
            System.exit(EXIT_SUCCESS);
        }
        else
        {
            System.err.println(serverType() + " server stopped due to an " +
                               "exception");
            stop_cause.printStackTrace(System.err);
            System.exit(EXIT_FAILURE);
        }
    }

    /** Schedules a task that will hard-terminate the JVM if the server fails to
        stop gracefully. */
    protected void startTerminationTimer()
    {
        termination_timeout_timer.schedule(new TerminationTimeoutTask(),
                                           TERMINATION_TIMEOUT);
    }

    /** Indicates that the server has stopped and wakes the waiting main thread.

        @param cause Exception that caused the server to stop, or
                     <code>null</code> if the server stopped normally.
     */
    protected synchronized void serverStopped(Throwable cause)
    {
        // Prevent repeat assignments of cause.
        if(stopped)
            return;

        // Mark the server as stopped and cancel the timeout timer task.
        stop_cause = cause;
        stopped = true;

        termination_timeout_timer.cancel();

        // Wake the main thread.
        notifyAll();
    }

    /** Thread that monitors standard input for end-of-file. */
    private class EOFThread implements Runnable
    {
        /** Monitors standard input for end-of-file. */
        @Override
        public void run()
        {
            // Repeatedly try to read from standard input. If the return value
            // indicates end-of-file, or if an exception occurs, stop the
            // server.
            try
            {
                while(System.in.read() != -1)
                    continue;
            }
            catch(IOException e) { }

            stopServer();
        }
    }

    /** Timer task to terminate the virtual machine if the server does not
        terminate gracefully. */
    private class TerminationTimeoutTask extends TimerTask
    {
        /** Terminates the virtual machine if the server has not yet stopped. */
        @Override
        public void run()
        {
            // Take the lock on the ServerApplication object. This is done to
            // prevent a race condition between this method and serverStopped.
            synchronized(ServerApplication.this)
            {
                if(stopped)
                    return;

                System.err.println(serverType() + " server timed out while " +
                                   "stopping");
                System.exit(EXIT_FAILURE);
            }
        }
    }

    /** Indicates that the command line arguments supplied to the application
        are malformed. */
    protected class BadUsageException extends Exception
    {
        /** Creates a <code>BadUsageException</code> with the given usage
            string. */
        protected BadUsageException(String usage_string)
        {
            super(usage_string);
        }
    }
}
