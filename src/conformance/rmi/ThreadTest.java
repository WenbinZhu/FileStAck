package conformance.rmi;

import test.*;
import rmi.*;
import java.net.*;

/** Checks that the skeleton supports multiple simultaneous connections.

    <p>
    These tests are best performed after <code>SkeletonTest</code> and
    <code>StubTest</code>. This test starts a skeleton and creates a stub of
    type <code>TestInterface</code>. It then calls <code>rendezvous</code> on
    the stub from two different threads. The test succeeds if both calls return.
 */
public class ThreadTest extends Test
{
    /** Test notice. */
    public static final String  notice = "checking skeleton multithreading";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {SkeletonTest.class, StubTest.class};

    /** Server object used in the test. */
    private TestServer          server;
    /** Skeleton object used in the test. */
    private TestSkeleton        skeleton;
    /** Stub through which communication with the server occurs. */
    private TestInterface       stub;

    /** Initializes the test. */
    @Override
    protected void initialize() throws TestFailed
    {
        server = new TestServer();
        skeleton = new TestSkeleton();

        try
        {
            skeleton.start();
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start skeleton", t);
        }
    }

    /** Performs the test.

        @throws TestFailed If the test fails.
     */
    @Override
    protected void perform() throws TestFailed
    {
        // Create the stub.
        try
        {
            stub = Stub.create(TestInterface.class, skeleton);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create stub", t);
        }

        // Start a second thread that calls rendezvous on the test server.
        new Thread(new SecondThread()).start();

        // Call rendezvous on the test server.
        try
        {
            stub.rendezvous();
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to rendezvous in first thread", t);
        }
    }

    /** Stops the skeleton server. */
    @Override
    protected void clean()
    {
        skeleton.stop();
        skeleton = null;
    }

    /** Wakes the other thread, which is waiting for the reply from the
        server. */
    private class SecondThread implements Runnable
    {
        /** Calls the <code>wake</code> method on the remote server. */
        @Override
        public void run()
        {
            try
            {
                stub.rendezvous();
            }
            catch(Throwable t)
            {
                failure(new TestFailed("unable to rendezvous in second " +
                                       "thread", t));
            }
        }
    }

    /** Test skeleton class that fails the test when an exception is received in
        one of the skeleton's threads. */
    private class TestSkeleton extends Skeleton<TestInterface>
    {
        /** Creates a <code>TestSkeleton</code> with a new server object. */
        TestSkeleton()
        {
            super(TestInterface.class, server);
        }

        /** Wakes any threads blocked in the server. */
        @Override
        protected void stopped(Throwable cause)
        {
            server.wake();
        }

        /** Fails the test upon an error in the listening thread. */
        @Override
        protected boolean listen_error(Exception e)
        {
            failure(new TestFailed("exception in listening thread", e));

            return false;
        }

        /** Fails the test upon an error in a service thread. */
        @Override
        protected void service_error(RMIException e)
        {
            failure(new TestFailed("exception in service thread", e));
        }
    }
}
