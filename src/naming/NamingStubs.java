package naming;

import java.net.*;

import rmi.*;

/** Default port numbers for the naming server and convenience methods for
    making naming server stubs. */
public abstract class NamingStubs
{
    /** Default naming server client service port. */
    public static final int     SERVICE_PORT = 6000;
    /** Default naming server registration port. */
    public static final int     REGISTRATION_PORT = 6001;

    /** Returns a stub for a naming server client service interface.

        @param hostname Naming server hostname.
        @param port Client service interface port.
     */
    public static Service service(String hostname, int port)
    {
        InetSocketAddress   address = new InetSocketAddress(hostname, port);
        return Stub.create(Service.class, address);
    }

    /** Returns a stub for a naming server client service interface.

        <p>
        The default port is used.

        @param hostname Naming server hostname.
     */
    public static Service service(String hostname)
    {
        return service(hostname, SERVICE_PORT);
    }

    /** Returns a stub for a naming server registration interface.

        @param hostname Naming server hostname.
        @param port Registration interface port.
     */
    public static Registration registration(String hostname, int port)
    {
        InetSocketAddress   address = new InetSocketAddress(hostname, port);
        return Stub.create(Registration.class, address);
    }

    /** Returns a stub for a naming server registration interface.

        <p>
        The default port is used.

        @param hostname Naming server hostname.
     */
    public static Registration registration(String hostname)
    {
        return registration(hostname, REGISTRATION_PORT);
    }
}
