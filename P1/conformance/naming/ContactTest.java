package conformance.naming;

import java.io.*;
import java.net.*;

import test.*;
import naming.*;

/** Tests that the naming server is listening on the correct ports.

    <p>
    This test creates two sockets and attempts to connect to the naming server
    client service and registration ports. The test succeeds if both connections
    are accepted.
 */
public class ContactTest extends NamingTest
{
    /** Test notice. */
    public static final String  notice =
        "checking naming server listening ports";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {conformance.rmi.SkeletonTest.class};

    /** Performs the test.

        @throws TestFailed If the test fails.
     */
    @Override
    protected void perform() throws TestFailed
    {
        probe(NamingStubs.SERVICE_PORT, "service");
        probe(NamingStubs.REGISTRATION_PORT, "registration");
    }

    /** Attempts to connect to a listening socket at the given port.

        <p>
        The connection is attempted to the local host.

        @param port The port on which to request connection.
        @param interface_name The name of the interface to which the connection
                              is being made. This is used for the error message
                              in case an exception occurs.
        @throws TestFailed If the connection attempt fails.
     */
    private void probe(int port, String interface_name) throws TestFailed
    {
        Socket  service_socket = new Socket();

        // Attempt to make the connection.
        try
        {
            service_socket.connect(new InetSocketAddress("127.0.0.1", port));
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to contact naming server on " +
                                 interface_name + " interface", t);
        }

        // Make a best effort to close the socket if the connection is
        // successful.
        try
        {
            service_socket.close();
        }
        catch(IOException e) { }
    }
}
