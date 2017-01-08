package conformance.rmi;

import test.*;
import rmi.*;
import java.net.*;

/** Performs basic tests on the public interface of {@link rmi.Stub}.

    <p>
    These tests are best performed after <code>SkeletonTest</code>.

    <p>
    The tests performed are:
    <ul>
    <li>Stubs cannot be created for classes.</li>
    <li>Stubs cannot be created for non-remote interfaces.</li>
    <li>Stubs cannot be created with <code>null</code> arguments to
        <code>Stub.create</code>.</li>
    <li>Stubs connect to the address with which they are created.</li>
    <li>Stubs correctly implement <code>equals</code> and
        <code>hashCode</code>.</li>
    <li>Stubs implement the <code>toString</code> method.</li>
    </ul>
 */
public class StubTest extends Test
{
    /** Test notice. */
    public static final String  notice = "checking stub public interface";
    /** Prerequisites. */
    public static final Class[] prerequisites = new Class[]
        {SkeletonTest.class};

    /** Socket address used for the creation of stubs. */
    private InetSocketAddress           address;
    /** Dummy skeleton used during the construction of stubs. */
    private Skeleton<TestInterface>     skeleton;
    /** Server socket used by the listening thread for the connection check. */
    private ServerSocket                socket;
    /** When <code>false</code>, the connection check test is complete. */
    private boolean                     listening;

    /** Initializes the test.

        <p>
        This method creates the listening socket and dummy skeleton used in the
        test.
     */
    @Override
    protected void initialize() throws TestFailed
    {
        address = new InetSocketAddress(7000);
        listening = true;

        try
        {
            socket = new ServerSocket();
        }
        catch(Exception e)
        {
            throw new TestFailed("unable to create listening socket", e);
        }

        // Create a dummy server and the dummy skeleton.
        TestServer  server = new TestServer();

        try
        {
            skeleton = new Skeleton<TestInterface>(TestInterface.class, server);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create dummy skeleton", t);
        }
    }

    /** Performs the test. */
    @Override
    protected void perform() throws TestFailed
    {
        ensureUnknownHostRejected();

        try
        {
            skeleton.start();
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start dummy skeleton", t);
        }

        ensureClassRejected();
        ensureNonRemoteInterfaceRejected();
        ensureNullPointerExceptions();
        ensureLocalMethods();

        skeleton.stop();

        ensureStubConnects();
    }

    /** Checks that a stub connects to the server for which it was created.

        @throws TestFailed If the stub does not connect.
     */
    private void ensureStubConnects() throws TestFailed
    {
        TestInterface   stub;

        // Create the stub.
        try
        {
            stub = Stub.create(TestInterface.class, address);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create stub for connecting to " +
                                 "test server", t);
        }

        // Bind the listening socket.
        try
        {
            socket.bind(address);
        }
        catch(Exception e)
        {
            throw new TestFailed("unable to bind listening socket to " +
                                 "address", e);
        }

        // Start the listening thread. The thread will not be able to call wake
        // until this function calls wait.
        new Thread(new ConnectionCheckThread()).start();

        // Attempt to connect to the listening server.
        try
        {
            stub.method(false);
        }
        catch(RMIException e)
        {
            return;
        }
        catch(Throwable t)
        {
            throw new TestFailed("exception when attempting to connect to " +
                                 "server", t);
        }

        throw new TestFailed("stub sent no data");
    }

    /** Stops the dummy skeleton and testing server. */
    @Override
    protected void clean()
    {
        skeleton.stop();

        try
        {
            socket.close();
        }
        catch(Exception e) { }
    }

    /** Ensures that a stub cannot be created from a skeleton whose address has
        not been determined.

        <p>
        This method should be used before the test skeleton is started.

        @throws TestFailed If <code>IllegalStateException</code> is not thrown.
     */
    private void ensureUnknownHostRejected() throws TestFailed
    {
        try
        {
            TestInterface   stub = Stub.create(TestInterface.class, skeleton);
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>) allowed " +
                                 "stub to be created from skeleton with " +
                                 "unassigned address");
        }
        catch(TestFailed e) { throw e; }
        catch(IllegalStateException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>) threw " +
                                 "an unexpected exception when given a " +
                                 "skeleton with an unassigned address", t);
        }
    }

    /** Ensures that a <code>Stub</code> cannot be created from a class rather
        than an interface.

        @throws TestFailed If a <code>Stub</code> is created from a class, or if
                           an unexpected exception occurs.
     */
    private void ensureClassRejected() throws TestFailed
    {
        try
        {
            Object          stub = Stub.create(Object.class, address);
            throw new TestFailed("Stub.create(Class<T>, InetSocketAddress) " +
                                 "has accepted a class");
        }
        catch(TestFailed e) { throw e; }
        catch(Error e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, InetSocketAddress) " +
                                 "constructor threw an unexpected exception " +
                                 "when given a class", t);
        }
    }

    /** Ensures that a <code>Stub</code> cannot be created from a non-remote
        interface.

        @throws TestFailed If a <code>Stub</code> is created from a non-remote
                           interface, or if an unexpected exception occurs.
     */
    private void ensureNonRemoteInterfaceRejected() throws TestFailed
    {
        try
        {
            BadInterface    stub = Stub.create(BadInterface.class, address);
            throw new TestFailed("Stub.create(Class<T>, InetSocketAddress) " +
                                 "has accepted a non-remote interface");
        }
        catch(TestFailed e) { throw e; }
        catch(Error e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, InetSocketAddress) " +
                                 "constructor threw an unexpected exception " +
                                 "when given a non-remote interface", t);
        }
    }

    /** Ensures that both <code>Stub.create</code> methods throw
        <code>NullPointerException</code> when given <code>null</code> for any
        parameters.

        @throws TestFailed If <code>null</code> is given as a parameter but the
                           correct exception is not thrown.
     */
    private void ensureNullPointerExceptions() throws TestFailed
    {
        // Make sure that null for the first argument is rejected.
        try
        {
            TestInterface   stub = Stub.create(null, skeleton);
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>) " +
                                 "accepted null for first argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>) threw " +
                                 "an unexpected exception when given null " +
                                 "for first argument", t);
        }

        try
        {
            TestInterface   stub = Stub.create(null, skeleton, "127.0.0.1");
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>, String) " +
                                 "accepted null for first argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>, String) " +
                                 "thew an unexpected exception when given " +
                                 "null for first argument", t);
        }

        try
        {
            TestInterface   stub = Stub.create(null, address);
            throw new TestFailed("Stub.create(Class<T>, InetSocketAddress) " +
                                 "accepted null for first argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, InetSocketAddress) " +
                                 "threw an unexpected exception when given " +
                                 "null for first argument", t);
        }

        // Make sure that null for the second argument is rejected.
        try
        {
            TestInterface   stub =
                Stub.create(TestInterface.class, (Skeleton<TestInterface>)null);
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>) " +
                                 "accepted null for second argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>) threw " +
                                 "an unexpected exception when given null " +
                                 "for second argument", t);
        }

        try
        {
            TestInterface   stub =
                Stub.create(TestInterface.class, (Skeleton<TestInterface>)null,
                            "127.0.0.1");
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>, String) " +
                                 "accepted null for second argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>, String) " +
                                 "threw an unexpected exception when given " +
                                 "null for second argument", t);
        }

        try
        {
            TestInterface   stub =
                Stub.create(TestInterface.class, (InetSocketAddress)null);
            throw new TestFailed("Stub.create(Class<T>, InetSocketAddress) " +
                                 "accepted null for second argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, InetSocketAddress) " +
                                 "threw an unexpected exception when given " +
                                 "null for second argument", t);
        }

        // Make sure that the three-argument form of create rejects null for the
        // third argument.
        try
        {
            TestInterface   stub =
                Stub.create(TestInterface.class, skeleton, null);
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>, String) " +
                                 "accepted null for third argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Stub.create(Class<T>, Skeleton<T>, String) " +
                                 "threw an unexpected exception when given " +
                                 "null for third argument", t);
        }
    }

    /** Ensures that stubs for the same skeleton are equal and have the same
        hash code, and stubs have a string representation.

        @throws TestFailed If the test fails.
     */
    private void ensureLocalMethods() throws TestFailed
    {
        // Create two stubs for the same skeleton.
        TestInterface   stub1;
        TestInterface   stub2;

        try
        {
            stub1 = Stub.create(TestInterface.class, skeleton);
            stub2 = Stub.create(TestInterface.class, skeleton);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create stub", t);
        }

        // Create a third stub for a different skeleton. The port is in the
        // reserved port range, so it is not one of the ports that the system
        // may automatically assign to the other skeletons (and therefore the
        // other two stubs).
        TestInterface   stub3 =
            Stub.create(TestInterface.class, new InetSocketAddress(80));

        // Check that stubs are not equal to null.
        try
        {
            if(stub1.equals(null))
                throw new TestFailed("stub is reported as equal to null");
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("equals threw an unexpected exception when " +
                                 "comparing a stub to null", t);
        }

        // Check that the first two stubs are equal.
        try
        {
            if(!stub1.equals(stub2))
            {
                throw new TestFailed("stubs for the same skeleton are not " +
                                     "equal");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("equals threw an unexpected exception when " +
                                 "comparing equal stubs", t);
        }

        // Check that the first stub is different from the last.
        try
        {
            if(stub1.equals(stub3))
                throw new TestFailed("stubs for different skeletons are equal");
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("equals threw an unexpected exception when " +
                                 "comparing unequal stubs", t);
        }

        // Check that the first two stubs have the same hash code.
        try
        {
            if(stub1.hashCode() != stub2.hashCode())
                throw new TestFailed("equal stubs have different hash codes");
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("hashCode threw an unexpected exception", t);
        }

        // Check that the first and third stubs have different hash codes.
        try
        {
            if(stub1.hashCode() == stub3.hashCode())
                throw new TestFailed("unequal stubs have the same hash code");
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("hashCode threw an unexpected exception", t);
        }

        // Check that the toString method is implemented.
        try
        {
            stub1.toString();
        }
        catch(Throwable t)
        {
            throw new TestFailed("toString threw an unexpected exception", t);
        }
    }

    /** Thread listening for a connection. */
    private class ConnectionCheckThread implements Runnable
    {
        /** Accepts one connection at the server socket, then closes the
            connected socket. */
        @Override
        public void run()
        {
            try
            {
                Socket  connected = socket.accept();

                try
                {
                    connected.close();
                }
                catch(Exception e) { }
            }
            catch(Exception e)
            {
                // An exception may be generated due to a genuine error, or
                // because clean has already been called. If clean has already
                // been called, this call to failure will have no effect.
                failure(new TestFailed("caught an exception while listening " +
                                       "for a connection", e));
            }
        }
    }
}
