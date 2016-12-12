package conformance.rmi;

import test.*;
import rmi.*;
import java.net.*;

/** Performs basic tests on the public interface of {@link rmi.Skeleton}.

    <p>
    The tests performed are:
    <ul>
    <li>Both <code>Skeleton</code> constructors reject classes.</li>
    <li>Both constructors reject non-remote interfaces.</li>
    <li>Both constructors require the first two arguments to be
        non-<code>null</code>.</li>
    <li>The skeleton can be started and stopped, and accepts connections while
        started.</li>
    </ul>
 */
public class SkeletonTest extends Test
{
    /** Test notice. */
    public static final String  notice = "checking skeleton public interface";

    /** Socket address used for the creation of skeletons. */
    private final InetSocketAddress             address;
    /** Dummy object used for testing calls to <code>Skeleton</code>
        constructors. */
    private final BadInterfaceImplementation    dummy_server;
    /** Regular server object. */
    private final TestServer                    server;
    /** The main skeleton used for testing. */
    private final TestSkeleton                  skeleton;
    /** Indicates whether the skeleton has stopped. */
    private boolean                             stopped;

    /** Creates a <code>SkeletonTest</code> object. */
    public SkeletonTest()
    {
        address = new InetSocketAddress(7000);
        dummy_server = new BadInterfaceImplementation();
        server = new TestServer();
        skeleton = new TestSkeleton();
        stopped = false;
    }

    /** Performs the test. */
    @Override
    protected void perform() throws TestFailed
    {
        ensureClassRejected();
        ensureNonRemoteInterfaceRejected();
        ensureNullPointerExceptions();
        ensureSkeletonRuns();
    }

    /** Performs tests with a running skeleton.

        <p>
        This method starts the skeleton and then stops it. In between, it probes
        to see if the skeleton is accepting connections.
     */
    private void ensureSkeletonRuns() throws TestFailed
    {
        if(probe())
            throw new TestFailed("skeleton accepts connections before start");

        try
        {
            skeleton.start();
        }
        catch(RMIException e)
        {
            throw new TestFailed("unable to start skeleton", e);
        }

        if(!probe())
            throw new TestFailed("skeleton refuses connections after start");

        skeleton.stop();

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

        if(probe())
            throw new TestFailed("skeleton accepts connections after stop");
    }

    /** Wakes <code>ensureSkeletonRuns</code>. */
    private synchronized void wake()
    {
        stopped = true;
        notifyAll();
    }

    /** Checks that it is possible to connect to the server.

        @return <code>true</code> if the connection can be established, and
                <code>false</code> if it cannot be.
     */
    private boolean probe()
    {
        Socket      socket = new Socket();

        try
        {
            socket.connect(address);
        }
        catch(Exception e)
        {
            return false;
        }

        try
        {
            socket.close();
        }
        catch(Exception e) { }

        return true;
    }

    /** Stops the skeleton server, if it is running, and attempts to wake the
        test main thread. */
    @Override
    protected void clean()
    {
        skeleton.stop();
        wake();
    }

    /** Ensures that a <code>Skeleton</code> cannot be constructed from a class
        rather than an interface.

        @throws TestFailed If a <code>Skeleton</code> is constructed from a
                           class, or if an unexpected exception occurs.
     */
    private void ensureClassRejected() throws TestFailed
    {
        try
        {
            Skeleton<Object>    bad_skeleton =
                new Skeleton<Object>(Object.class, dummy_server);
            throw new TestFailed("Skeleton(Class<T>, T) constructor has " +
                                 "accepted a class");
        }
        catch(TestFailed e) { throw e; }
        catch(Error e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Skeleton(Class<T>, T) constructor threw " +
                                 "an unexpected exception when given a " +
                                 "class", t);
        }

        try
        {
            Skeleton<Object>    bad_skeleton =
                new Skeleton<Object>(Object.class, dummy_server, address);
            throw new TestFailed("Skeleton(Class<T>, T, InetSocketAddress) " +
                                 "constructor has accepted a class");
        }
        catch(TestFailed e) { throw e; }
        catch(Error e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Skeleton(Class<T>, T, InetSocketAddress) " +
                                 "constructor threw an unexpected exception " +
                                 "when given a class", t);
        }
    }

    /** Ensures that a <code>Skeleton</code> cannot be constructed from a
        non-remote interface.

        @throws TestFailed If a <code>Skeleton</code> is constructed from a
                           non-remote interface, or if an unexpected exception
                           occurs.
     */
    private void ensureNonRemoteInterfaceRejected() throws TestFailed
    {
        try
        {
            Skeleton<BadInterface>  bad_skeleton =
                new Skeleton<BadInterface>(BadInterface.class, dummy_server);
            throw new TestFailed("Skeleton(Class<T>, T) constructor has " +
                                 "accepted a non-remote interface");
        }
        catch(TestFailed e) { throw e; }
        catch(Error e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Skeleton(Class<T>, T) constructor threw " +
                                 "an unexpected exception when given a " +
                                 "non-remote interface", t);
        }

        try
        {
            Skeleton<BadInterface>  bad_skeleton =
                new Skeleton<BadInterface>(BadInterface.class, dummy_server,
                                           address);
            throw new TestFailed("Skeleton(Class<T>, T, InetSocketAddress) " +
                                 "constructor has accepted a non-remote " +
                                 "interface");
        }
        catch(TestFailed e) { throw e; }
        catch(Error e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Skeleton(Class<T>, T, InetSocketAddress) " +
                                 "constructor threw an unexpected exception " +
                                 "when given a non-remote interface", t);
        }
    }

    /** Ensures that <code>Skeleton</code> constructors throw
        <code>NullPointerException</code> when given <code>null</code> for the
        class or server parameters.

        @throws TestFailed If <code>null</code> is given as a parameter but the
                           correct exception is not thrown.
     */
    private void ensureNullPointerExceptions() throws TestFailed
    {
        // Make sure that null for the first argument is rejected.
        try
        {
            Skeleton<TestInterface>    bad_skeleton =
                new Skeleton<TestInterface>(null, server);
            throw new TestFailed("Skeleton(Class<T>, T) constructor " +
                                 "accepted null for first argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Skeleton(Class<T>, T) constructor threw " +
                                 "an unexpected exception when given null " +
                                 "for first argument", t);
        }

        try
        {
            Skeleton<TestInterface>    bad_skeleton =
                new Skeleton<TestInterface>(null, server, address);
            throw new TestFailed("Skeleton(Class<T>, T, InetSocketAddress) " +
                                 "constructor accepted null for first " +
                                 "argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Skeleton(Class<T>, T, InetSocketAddress) " +
                                 "constructor threw an unexpected exception " +
                                 "when given null for first argument", t);
        }

        // Make sure that null for the second argument is rejected.
        try
        {
            Skeleton<TestInterface>    bad_skeleton =
                new Skeleton<TestInterface>(TestInterface.class, null);
            throw new TestFailed("Skeleton(Class<T>, T) constructor " +
                                 "accepted null for second argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Skeleton(Class<T>, T) constructor threw " +
                                 "an unexpected exception when given null " +
                                 "for second argument", t);
        }

        try
        {
            Skeleton<TestInterface>    bad_skeleton =
                new Skeleton<TestInterface>(TestInterface.class, null,
                                            address);
            throw new TestFailed("Skeleton(Class<T>, T, InetSocketAddress) " +
                                 "constructor accepted null for second " +
                                 "argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Skeleton(Class<T>, T, InetSocketAddress) " +
                                 "constructor threw an unexpected exception " +
                                 "when given null for second argument", t);
        }
    }

    /** Derivative of <code>Skeleton</code> which notifies the test when it
        stops.

        <p>
        Service thread errors are ignored because, in this test, their source is
        generally the <code>probe</code> method.
     */
    private class TestSkeleton extends Skeleton<TestInterface>
    {
        /** Creates a <code>TestSkeleton</code> */
        TestSkeleton()
        {
            super(TestInterface.class, server, address);
        }

        /** Wakes the testing main thread. */
        @Override
        protected void stopped(Throwable cause)
        {
            wake();
        }

        /** Handles an error in the listening thread. */
        @Override
        protected boolean listen_error(Exception e)
        {
            failure(new TestFailed("error in skeleton listening thread", e));

            return false;
        }
    }

    /** Dummy implementation of <code>BadInterface</code>. */
    private class BadInterfaceImplementation implements BadInterface
    {
        /** Returns its argument. */
        @Override
        public Object method(int argument)
        {
            return argument;
        }
    }
}
