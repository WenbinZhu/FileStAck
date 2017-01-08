package conformance.naming;

import java.net.*;

import rmi.*;
import test.*;
import naming.*;

/** Base class of naming server tests.

    <p>
    On initialization, this class starts a naming server and creates stubs for
    its service and registration interfaces. These objects are then accessible
    to subclasses of <code>NamingTest</code>.

    <p>
    Derived classes should override <code>testServer</code> to include the main
    test code. Derived classes should override <code>initialize</code> and
    <code>clean</code> if they have additional servers or system objects to
    create, start, stop, and clean up.
 */
abstract class NamingTest extends Test
{
    /** Naming server under test. */
    private TestNamingServer    server = null;
    /** Stub for naming server client service interface. */
    protected Service           service_stub = null;
    /** Stub for naming server registration interface. */
    protected Registration      registration_stub = null;
    /** Indicates that the naming server has stopped. */
    private boolean             stopped = false;

    /** Initializes the test.

        <p>
        This method starts the naming server and creates the stubs through which
        its interfaces can be accessed.

        @throws TestFailed If the naming server cannot be started, or if the
                           stubs cannot be created.
     */
    protected void initialize() throws TestFailed
    {
        // Create the naming server object.
        try
        {
            server = new TestNamingServer();
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create naming server", t);
        }

        // Start the naming server.
        try
        {
            server.start();
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start naming server", t);
        }

        // Create the service interface stub.
        try
        {
            InetSocketAddress   address =
                new InetSocketAddress("127.0.0.1", NamingStubs.SERVICE_PORT);
            service_stub = Stub.create(Service.class, address);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create service stub", t);
        }

        // Create the registration interface stub.
        try
        {
            InetSocketAddress   address =
                new InetSocketAddress("127.0.0.1",
                                      NamingStubs.REGISTRATION_PORT);
            registration_stub = Stub.create(Registration.class, address);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create registration stub", t);
        }
    }

    /** Stops the naming server when the test completes.

        <p>
        If a subclass overrides this method, the new implementation should call
        this method before proceeding to do anything else.
     */
    @Override
    protected void clean()
    {
        if(server != null)
        {
            server.stop();
            server = null;

            // Wait for the naming server to stop.
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
        }
    }

    /** Naming server class, subclassed to notify the test when the naming
        server stops. */
    private class TestNamingServer extends NamingServer
    {
        /** Wakes any thread waiting in <code>clean</code>. */
        @Override
        protected void stopped(Throwable cause)
        {
            synchronized(NamingTest.this)
            {
                stopped = true;
                NamingTest.this.notifyAll();
            }
        }
    }
}
