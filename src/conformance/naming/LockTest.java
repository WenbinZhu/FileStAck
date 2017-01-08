package conformance.naming;

import java.io.*;
import java.util.*;

import test.*;
import common.*;

/** Tests basic functioning of the naming server <code>lock</code> and
    <code>unlock</code> methods.

    <p>
    Properties checked are:
    <ul>
    <li>The <code>lock</code> and <code>unlock</code> methods reject
        <code>null</code> paths and paths to non-existent objects.</li>
    <li>A lock cannot be taken simultaneously for exclusive access by one
        thread, and any kind of access by another.</li>
    <li>A lock can be taken for shared access by two threads at once.</li>
    <li>Two threads can obtain exclusive access to siblings in the directory
        tree simultaneously.</li>
    <li>Taking an exclusive lock on an object prevents child objects from being
        locked.</li>
    </ul>
 */
public class LockTest extends NamingTest
{
    /** Test notice. */
    public static final String  notice =
        "checking naming server lock sharing and exclusion";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {RegistrationTest.class};

    /** Storage server used in the test. The purpose of this storage server is
        merely to register some files. It is not used otherwise. */
    private TestStorageServer   storage_server = null;

    // Several paths used in the test.
    private final Path          root = new Path("/");
    private final Path          directory = new Path("/directory");
    private final Path          file1 = new Path("/directory/file1");
    private final Path          file2 = new Path("/directory/file2");

    /** For a rendezvous test, indicates that the next thread to take the lock
        is the first thread and should wait for the second. */
    private boolean             rendezvous_wait;
    /** Indicates that the second thread successfully took the lock in the
        rendzevous test. */
    private boolean             rendezvous_wake;
    /** Number of threads that have completed the rendezvous test. The main
        testing thread waits until this number is equal to two. */
    private int                 rendezvous_exits;

    /** Indicates that the first thread in the exclusive access test has taken
        the lock, and therefore the second thread may go on with its attempt to
        take it as well. */
    private boolean             exclusive_locked;
    /** Indicates that the first thread in the exclusive access test has
        released the lock. If the second thread is able to take the lock while
        this field is false, the two threads were able to take the lock at the
        same time. */
    private boolean             exclusive_resume;
    /** Indicates that the second thread has released the lock, and therefore
        the exclusive access test is finished. */
    private boolean             exclusive_finished;
    /** Minimum amount of time, in milliseconds, that the first thread in the
        exclusive access test holds the lock before releasing it. This delay is
        long enough to make it very likely that the second thread will try to
        take the lock while the first thread still holds it. */
    private static final int    EXCLUSIVE_TEST_DELAY = 250;

    /** Indicates that the test has completed and all waits are cancelled. */
    private boolean             wake_all = false;

    /** Performs the test. */
    @Override
    protected void perform() throws TestFailed
    {
        testBadPaths();

        testSharing(root, false, root, false);
        testSharing(file1, true, file2, true);
        testSharing(file1, true, directory, false);

        testExclusion(root, false, root, true);
        testExclusion(root, true, root, false);
        testExclusion(root, true, root, true);
        testExclusion(root, true, directory, false);
        testExclusion(directory, false, root, true);
        testExclusion(directory, true, root, true);
    }

    /** Checks that two threads are able to lock the two given paths
        simultaneously for the requested kind of access.

        <p>
        Two threads are started, and each attempts to lock its given object. In
        order for the test to succeed, the two threads must rendezvous with the
        lock taken. The first thread to lock an object waits for the second
        thread to also lock its object. Only after both threads have
        successfully locked both their objects are they allowed to proceed. The
        test is successful when both threads have unlocked their respective
        objects.

        <p>
        The code run by the two threads is identical. What determines their
        behavior (whether to wait for the second thread, or to wake the first)
        is the order in which they take the lock.

        @param first_path Path to the object to be locked by the first thread.
        @param first_exclusive <code>true</code> if the first thread is to take
                               its object for exclusive access,
                               <code>false</code> if the first thread is to
                               request shared access.
        @param second_path Path to the object to be locked by the second thread.
        @param second_exclusive <code>true</code> if the second thread is to
                                take its object for exclusive access,
                                <code>false</code> if the second thread is to
                                request shared access.
     */
    private void testSharing(Path first_path, boolean first_exclusive,
                             Path second_path, boolean second_exclusive)
    {
        // Set the current task message in case the test times out. This is the
        // most common type of failure.
        task("attempting to lock " + first_path + " for " +
             accessType(first_exclusive) + " access and " + second_path +
             " for " + accessType(second_exclusive) + " access simultaneously");

        // Set flags that will control the behavior of each thread.
        rendezvous_wait = true;
        rendezvous_wake = false;
        rendezvous_exits = 0;

        // Start the two threads.
        new Thread(new RendezvousUser(first_path, first_exclusive)).start();
        new Thread(new RendezvousUser(second_path, second_exclusive)).start();

        // Wait for both threads to exit, or for the test to be terminated by
        // timeout.
        synchronized(this)
        {
            while(rendezvous_exits < 2 && !wake_all)
            {
                try
                {
                    wait();
                }
                catch(InterruptedException e) { }
            }
        }

        // Clear the test task message.
        task();
    }

    /** Checks that two threads are not able to simultaneously lock the two
        given objects for the requested kinds of access.

        <p>
        Two threads are started. Unlike in the rendzevous test, the two threads
        are different. There is a thread that is intended to be the first to
        lock its object, and the other thread should be second. The second
        thread waits until the first thread signals it has locked its object.
        In the meantime, the first thread sleeps for a fixed minimum amount of
        time, in order to make it very likely that the second thread will
        attempt to lock its object after it is done waiting for the first. If
        the second thread is able to successfully lock its object during this
        delay, then the test has failed.

        @param first_path Path to the object to be locked by the first thread.
        @param first_exclusive <code>true</code> if the first thread is to take
                               its object for exclusive access,
                               <code>false</code> if the first thread is to
                               request shared access.
        @param second_path Path to the object to be locked by the second thread.
        @param second_exclusive <code>true</code> if the second thread is to
                                take its object for exclusive access,
                                <code>false</code> if the second thread is to
                                request shared access.
     */
    private void testExclusion(Path first_path, boolean first_exclusive,
                               Path second_path, boolean second_exclusive)
    {
        // Set the current task message in case the test times out. This is the
        // most common type of failure.
        task("locking " + first_path + " for " + accessType(first_exclusive) +
             " access and " + second_path + " for " +
             accessType(second_exclusive) + " access");

        // Set flags that control the behavior of the threads.
        exclusive_locked = false;
        exclusive_resume = false;
        exclusive_finished = false;

        // Start the two threads.
        new Thread(new FirstExclusiveUser(first_path, first_exclusive)).start();
        new Thread(new SecondExclusiveUser(second_path,
                                           second_exclusive)).start();

        // Create a timer and a timeout task that will wake the first thread
        // after a fixed minimum time period. The first thread will be sleeping
        // with the lock taken. For the test to succeed, the second thread must
        // not take the lock during this sleep.
        Timer   timer = new Timer();
        timer.schedule(new WakeTask(), EXCLUSIVE_TEST_DELAY);

        // Wait for the second thread to release the lock. If the test succeeds,
        // the only way this can happen is if the first thread has also released
        // the lock, so both threads have exited. If the test fails, then
        // wake_all flag will be set as a result of the call to clean.
        synchronized(this)
        {
            while(!exclusive_finished && !wake_all)
            {
                try
                {
                    wait();
                }
                catch(InterruptedException e) { }
            }
        }

        // Cancel the wake task early if the test fails. In any case, the timer
        // must be cancelled.
        timer.cancel();

        // Clear the test task message.
        task();
    }

    /** Returns a string describing the type of access requested.

        @param exclusive Access type.
        @return <code>"exclusive"</code> if the argument is <code>true</code>,
                <code>"shared"</code> if the argument is <code>false</code>.
     */
    private String accessType(boolean exclusive)
    {
        if(exclusive)
            return "exclusive";
        else
            return "shared";
    }

    /** Checks that the <code>lock</code> and <code>unlock</code> methods
        reject bad arguments. */
    private void testBadPaths() throws TestFailed
    {
        // Check that lock and unlock reject null.
        try
        {
            service_stub.lock(null, false);
            throw new TestFailed("lock method accepted null for path argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("lock method threw unexpected exception " +
                                 "when given null for path argument", t);
        }

        try
        {
            service_stub.unlock(null, false);
            throw new TestFailed("unlock method accepted null for path " +
                                 "argument");
        }
        catch(TestFailed e) { throw e; }
        catch(NullPointerException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("unlock method threw unexpected exception " +
                                 "when given null for path argument", t);
        }

        // Check that lock and unlock reject bad paths.
        Path    non_existent_path = new Path("/another_file");

        try
        {
            service_stub.lock(non_existent_path, false);
            throw new TestFailed("lock method accepted bad path");
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("lock method threw unexpected exception " +
                                 "when given bad path", t);
        }

        try
        {
            service_stub.unlock(non_existent_path, false);
            throw new TestFailed("unlock method accepted bad path");
        }
        catch(TestFailed e) { throw e; }
        catch(IllegalArgumentException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("unlock method threw unexpected exception " +
                                 "when given bad path", t);
        }
    }

    /** Initializes the test.

        <p>
        This method starts the storage server, creating a small directory tree
        on the naming server.

        @throws TestFailed If the storage server cannot be started.
     */
    @Override
    protected void initialize() throws TestFailed
    {
        super.initialize();

        try
        {
            storage_server = new TestStorageServer(this);
            storage_server.start(registration_stub, new Path[] {file1, file2},
                                 null);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start test storage server", t);
        }
    }

    /** Stops all servers and wakes all threads. */
    @Override
    protected void clean()
    {
        super.clean();

        if(storage_server != null)
        {
            storage_server.stop();
            storage_server = null;
        }

        synchronized(this)
        {
            wake_all = true;
            notifyAll();
        }
    }

    /** Rendezvous test thread.

        <p>
        This type of thread is used to check that locks can be taken
        simultaneously, or one lock can be taken for shared access by two
        threads. Rendezvous threads are created two at a time. The first thread
        takes a lock and waits for the second to do the same. The second thread
        wakes the first, and then both threads release the lock and exit. If the
        second thread is unable to take the lock while the first thread still
        holds it, the test will fail due to timeout, as the second thread is
        blocked indefinitely.
     */
    private class RendezvousUser extends LockUser
    {
        /** Creates a rendezvous thread.

            @param path Path to be locked by this thread.
            @param exclusive Whether or not the path is to be locked for
                             exclusive access.
         */
        RendezvousUser(Path path, boolean exclusive)
        {
            super(path, exclusive);
        }

        /** Causes the two threads to rendezvous.

            <p>
            If this thread turns out to be the first to take the lock, it waits
            for the other thread to wake it. If it turns out to be the second,
            it wakes the other thread.
         */
        @Override
        protected void locked()
        {
            synchronized(LockTest.this)
            {
                // Check if this thread is first.
                if(rendezvous_wait)
                {
                    // If so, the other thread will be second.
                    rendezvous_wait = false;

                    // Wait until the other thread wakes this thread, or the
                    // test is terminated by timeout.
                    while(!rendezvous_wake && !wake_all)
                    {
                        try
                        {
                            LockTest.this.wait();
                        }
                        catch(InterruptedException e) { }
                    }
                }
                else
                {
                    // If this is the second thread, wake the first thread.
                    rendezvous_wake = true;
                    LockTest.this.notifyAll();
                }
            }
        }

        /** Increments the number of threads that have exited.

            <p>
            This is used to wake the main testing thread, which is waiting for
            both threads to release the lock.
         */
        @Override
        protected void released()
        {
            synchronized(LockTest.this)
            {
                ++rendezvous_exits;
                LockTest.this.notifyAll();
            }
        }
    }

    /** First thread in the exclusive access test.

        <p>
        This type of thread takes a lock, and then waits for some amount of
        time. This should be enough time for the second thread to try to take
        the lock. If the second thread does not block, and succeeds in taking
        the lock while the first thread is sleeping, the locks were taken
        simultaneously, and the test fails.
     */
    private class FirstExclusiveUser extends LockUser
    {
        /** Creates the thread object.

            @param path Path to be locked by this thread.
            @param exclusive Whether or not the path is to be locked for
                             exclusive access.
         */
        FirstExclusiveUser(Path path, boolean exclusive)
        {
            super(path, exclusive);
        }

        /** Notifies the second thread that it should try to take the lock, and
            then waits. */
        @Override
        protected void locked()
        {
            synchronized(LockTest.this)
            {
                // Notify the second thread.
                exclusive_locked = true;
                LockTest.this.notifyAll();

                // Wait. This wait is terminated by <code>WakeTask</code>.
                while(!exclusive_resume && !wake_all)
                {
                    try
                    {
                        LockTest.this.wait();
                    }
                    catch(InterruptedException e) { }
                }
            }
        }
    }

    /** Second thread in the exclusive access test.

        <p>
        This type of thread waits for the first thread to signal it has taken
        its lock. It then attempts to take its own lock. If it succeeds while
        the first thread still holds the lock, the test is failed.
     */
    private class SecondExclusiveUser extends LockUser
    {
        /** Creates the thread object.

            @param path Path to be locked by this thread.
            @param exclusive Whether or not the path is to be locked for
                             exclusive access.
         */
        SecondExclusiveUser(Path path, boolean exclusive)
        {
            super(path, exclusive);
        }

        /** Ensures that the second thread does not try to take the lock before
            the first thread has done successfully. */
        @Override
        protected void started()
        {
            synchronized(LockTest.this)
            {
                while(!exclusive_locked && !wake_all)
                {
                    try
                    {
                        LockTest.this.wait();
                    }
                    catch(InterruptedException e) { }
                }
            }
        }

        /** Fails the test if the second thread has taken the lock while the
            first thread is still waiting. */
        @Override
        protected void locked()
        {
            synchronized(LockTest.this)
            {
                if(!exclusive_resume)
                {
                    exclusive_resume = true;
                    LockTest.this.notifyAll();

                    failure(new TestFailed("second thread was not blocked"));
                }
            }
        }

        /** Indicates that the test is complete. */
        @Override
        protected void released()
        {
            synchronized(LockTest.this)
            {
                exclusive_finished = true;
                LockTest.this.notifyAll();
            }
        }
    }

    /** Wakes the first thread in the exclusive access test. */
    private class WakeTask extends TimerTask
    {
        /** Wakes the thread!!! */
        @Override
        public void run()
        {
            synchronized(LockTest.this)
            {
                while(!exclusive_locked && !wake_all)
                {
                    try
                    {
                        LockTest.this.wait();
                    }
                    catch(InterruptedException e) { }
                }

                exclusive_resume = true;
                LockTest.this.notifyAll();
            }
        }
    }

    /** Base class of threads used for both the rendezvous and the exclusive
        access tests. */
    private abstract class LockUser implements Runnable
    {
        /** Path to be locked by this thread. */
        private final Path      path;
        /** Indicates that the path is to be locked for exclusive access. If
            <code>false</code>, the path should be locked for shared access. */
        private final boolean   exclusive;

        /** Initialies the fields of the <code>LockUser</code> object. */
        protected LockUser(Path path, boolean exclusive)
        {
            this.exclusive = exclusive;
            this.path = path;
        }

        /** Called before the thread attempts to take the lock. */
        protected void started()
        {
        }

        /** Called immediately after the thread has taken the lock. */
        protected void locked()
        {
        }

        /** Called after the thread has released the lock. */
        protected void released()
        {
        }

        /** Runs the thread. */
        @Override
        public void run()
        {
            started();

            // Lock the path.
            try
            {
                service_stub.lock(path, exclusive);
            }
            catch(Throwable t)
            {
                failure(new TestFailed("unable to lock " + path, t));
                return;
            }

            locked();

            // Unlock the path.
            try
            {
                service_stub.unlock(path, exclusive);
            }
            catch(Throwable t)
            {
                failure(new TestFailed("unable to unlock " + path, t));
                return;
            }

            released();
        }
    }
}
