package conformance.rmi;

import test.*;
import rmi.*;
import java.net.*;
import java.io.FileNotFoundException;

/** Tests complete connection between stub and skeleton.

    <p>
    This test starts a skeleton. Two stubs are then created - one implicitly
    from the skeleton, and one by specifying the address directly. Both stubs
    are then tested by calling a method in each. The test covers the passing of
    arguments, transmission of return values, and transmission of remote
    exceptions.
 */
public class ConnectionTest extends Test
{
    /** Test notice. */
    public static final String  notice =
        "checking connection between stub and skeleton";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {SkeletonTest.class, StubTest.class};

    /** Address at which the test skeleton will run. */
    private InetSocketAddress   address;
    /** Skeleton object used in the test. */
    private TestSkeleton        skeleton;

    /** Initializes the test. */
    @Override
    protected void initialize() throws TestFailed
    {
        address = new InetSocketAddress(7000);
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

    /** Performs the test. */
    @Override
    protected void perform() throws TestFailed
    {
        // Create two stubs - one using the skeleton, and one by specifying the
        // address explicitly. Make sure both can connect to the skeleton and
        // communicate with it correctly.
        TestInterface   stub_implicit;
        TestInterface   stub_explicit;

        try
        {
            stub_implicit = Stub.create(TestInterface.class, skeleton);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create stub", t);
        }

        task("connecting to skeleton using stub made from that skeleton");

        testStub(stub_implicit);

        task();

        try
        {
            stub_explicit = Stub.create(TestInterface.class, address);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create stub", t);
        }

        task("connecting to skeleton using stub given an explicit address");

        testStub(stub_explicit);

        task();
    }

    /** Stops the skeleton server. */
    @Override
    protected void clean()
    {
        skeleton.stop();
        skeleton = null;
    }

    /** Runs tests with a given stub.

        <p>
        Attempts to use the stub to get a regular result and an exception from
        the server.

        @throws TestFailed If any of the tests fail.
     */
    private void testStub(TestInterface stub) throws TestFailed
    {
        // Attempt to get a value from the stub.
        try
        {
            if(stub.method(false) != null)
                throw new TestFailed("incorrect result from stub");
        }
        catch(Throwable t)
        {
            throw new TestFailed("unexpected exception when using stub", t);
        }

        // Attempt to get an exception.
        try
        {
            stub.method(true);
            throw new TestFailed("exception expected but not received from " +
                                 "stub");
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("unexpected exception when using stub", t);
        }
    }

    /** Test skeleton class that fails the test when an exception is received in
        one of the skeleton's threads. */
    private class TestSkeleton extends Skeleton<TestInterface>
    {
        /** Creates a <code>TestSkeleton</code> at the appropriate address, with
            a new server object. */
        TestSkeleton()
        {
            super(TestInterface.class, new TestServer(), address);
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
