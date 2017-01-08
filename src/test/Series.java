package test;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/** Test series.

    <p>
    A test series is a group of related tests. A <code>Series</code> object
    provides the <code>run</code> method, which attempts to run all the tests in
    the series, and generates a test report.

    <p>
    Tests are generally run in the order in which they are provided. However,
    some tests may explicitly declare other tests in the series as
    prerequisites. Tests that declare prerequisites are run only after the
    prerequisites have completed successfully. Note that a test with
    prerequisites is not run in three cases: the prerequisite has failed,
    the prerequisite has itself not run, or the prerequisite is not included in
    the test series. The requirement that all prerequisites be explicitly
    included in the the test series is deliberate. The test series object
    notionally captures all the tests that will be run as part of the series.

    <p>
    Upon completion of a test series, the results are provided in a
    <code>SeriesReport</code> object. The <code>SeriesReport</code> object
    allows the results to be formatted and printed to an output stream.

    <p>
    The tests constituting a test series are provided as a list of classes. A
    test object is only instantiated immediately before the test is run. This
    ensures that it is possible to construct a test series even if the
    constructor of a test object would raise an exception - because test objects
    need not be constructed to construct a test series, only to run the test.

    <p>
    When running a test, the test series object creates a new thread. The thread
    constructs the test, and calls the test's <code>initialize</code> and
    <code>perform</code> methods. The test series object also starts a parallel
    timeout thread. The main test thread, any other threads started by it, and
    the timeout thread then race to call <code>success</code> or
    <code>failure</code>. The first call to either method determines the outcome
    of the test.

    <p>
    Once either method is called, the test series object creates a thread that
    calls <code>clean</code> on the test object, and a parallel cleanup timeout
    thread. The threads again race - this time to call
    <code>cleanupSuccess</code> or <code>cleanupFailure</code>. In the event of
    a cleanup failure, the test series has suffered a fatal error, and no more
    tests will be run, even if there are other tests for which all prerequisites
    have completed successfully. This is to prevent failures in those tests
    caused by unreleased system resources. A fatal error typically causes the
    process running the test series to stop the Java runtime, forcing the
    release of the system resources.
 */
public class Series implements Serializable
{
    /** List of tests making up this test series. */
    private final Class<? extends Test>[]       tests;

    /** Creates a test series, given a list of test classes.

        @param tests List of test classes. This array is copied, so the original
                     may be modified.
        @throws NullPointerException If <code>tests</code> is <code>null</code>,
                                     or if any of the elements of
                                     <code>tests</code> is <code>null</code>.
     */
    public Series(Class<? extends Test>[] tests)
    {
        // Check that the tests array is not null.
        if(tests == null)
            throw new NullPointerException("tests array is null");

        // Check that none of the elements of the tests array is null.
        for(int test_index = 0; test_index < tests.length; ++test_index)
        {
            if(tests[test_index] == null)
            {
                throw new NullPointerException("test " + test_index + " in " +
                                               "the test array is null");
            }
        }

        // Copy the tests array and finish.
        this.tests = Arrays.copyOf(tests, tests.length);
    }

    /** Runs the test series.

        <p>
        Tests are run in the order they were given to the <code>Series</code>
        constructor, subject to constraints imposed by test prerequisites.

        @param timeout Test timeout interval, in seconds. If a test fails to
                       complete in this amount of time, it is considered to have
                       timed out. Likewise, if a test fails to clean up in this
                       amount of time, the cleanup process is considered to have
                       failed. Note that a test can take twice this period to
                       run: one period to time out the main test, and one more
                       period to time out the cleanup process.
        @param stream Output stream to receive testing progress messages. Each
                      message takes the form of the test class notice message,
                      or a default replacement if the test class does not have a
                      notice message. When the test is complete, either
                      <code>ok</code> or <code>failed</code> is printed to the
                      stream. If this argument is <code>null</code>, no progress
                      printing is done.
        @return A <code>SeriesReport</code> object listing the tests that have
                succeeded, tests that have failed, and tests that were not run
                due to prerequisites that were missing or did not complete
                successfully.
     */
    public SeriesReport run(int timeout, PrintStream stream)
    {
        // This method creates a test queue, which is a list of all the tests to
        // be run, in the order that they are provided to the constructor. It
        // then repeatedly goes through the test queue. For each test in the
        // queue, it checks if all prerequisites have completed successfully. If
        // so, the test is removed from the queue and run. The method stops when
        // no more tests can be removed from the queue.

        // Test queue.
        List<Class<? extends Test>>         test_queue =
            new LinkedList<Class<? extends Test>>(Arrays.asList(tests));
        // Flag indicating that at least one test has been removed from the
        // queue in the current iteration.
        boolean                             progress = true;

        // Tests that have completed successfully.
        List<Class<? extends Test>>         successful_tests =
            new LinkedList<Class<? extends Test>>();
        // Reports from tests that have failed.
        ArrayList<TestReport>               failure_reports =
            new ArrayList<TestReport>();
        // A common Timer object for timeouts for all the tests.
        Timer                               timeout_timer = new Timer();

    main_loop:
        while(progress)
        {
            // This flag will be set to true if at least one test is removed
            // from the queue and run.
            progress = false;

            // Go through the test queue.
            Iterator<Class<? extends Test>> test_iterator =
                test_queue.iterator();

            while(test_iterator.hasNext())
            {
                // For each test, if the test is ready to be run (all
                // prerequisites are satisfied), take the test out of the queue
                // and run it.
                Class<? extends Test>       test_class = test_iterator.next();

                if(ready(test_class, successful_tests))
                {
                    test_iterator.remove();
                    progress = true;

                    // If there is an output stream provided for progress
                    // printing, print the test notice.
                    if(stream != null)
                    {
                        stream.print(notice(test_class) + "...");
                        stream.flush();
                    }

                    // Run the test.
                    TestReport              result =
                        run(test_class, timeout_timer, timeout);

                    // If the test is successful, note this fact and continue to
                    // the next test, if there is one.
                    if(result.successful())
                    {
                        successful_tests.add(test_class);

                        if(stream != null)
                        {
                            stream.println(" ok");
                            stream.flush();
                        }

                        continue;
                    }

                    // Otherwise, the test failed.
                    if(stream != null)
                    {
                        stream.println(" failed");
                        stream.flush();
                    }

                    // Store the failure report.
                    failure_reports.add(result);

                    // If the failure was fatal, stop the main loop and exit the
                    // method.
                    if(result.fatal())
                        break main_loop;
                }
            }
        }

        // Stop the timeout timer monitor thread and cancel any pending timeout
        // tasks (there should be none).
        timeout_timer.cancel();

        // If a stream was provided, and at least one test was run, print a
        // blank line.
        if(stream != null)
        {
            if((successful_tests.size() > 0) || (failure_reports.size() > 0))
                stream.println();
        }

        // Create the series report object and return it.
        return new SeriesReport(successful_tests, test_queue, failure_reports);
    }

    /** Runs a single test.

        <p>
        This method performs the testing process, as explained in the class
        description.

        @param test_class Test class. The test object will be instantiated from
                          this class.
        @param timeout_timer <code>Timer</code> object to be used for scheduling
                             timeout tasks. This is created once in the public
                             <code>run</code> method to avoid creating a new
                             timer monitor thread every time a test is run.
        @param timeout Timeout interval. As described in the public
                       <code>run</code> method.
     */
    private TestReport run(Class<? extends Test> test_class,
                           Timer timeout_timer, int timeout)
    {
        // Create a TestState object for the test. This object is necessary
        // because test-related state exists before the test object itself is
        // even constructed.
        TestState       state = new TestState();

        // Create the main testing thread and start it.
        new Thread(new TestThread(test_class, state)).start();

        // Schedule the test timeout task.
        TimerTask       test_timeout = new TestTimeoutTask(state);
        timeout_timer.schedule(test_timeout, (long)timeout * 1000);

        // Wait until the test is stopped (success or failure is called by the
        // test, or timeout occurs).
        synchronized(state)
        {
            while(!state.stopped)
            {
                try
                {
                    state.wait();
                }
                catch(InterruptedException e) { }
            }
        }

        // Cancel the timeout task if it has not yet run.
        test_timeout.cancel();

        // Check if the test object has been constructed. If the thread that
        // constructed the test object found that the test had been stopped
        // before the constructor finished, it left state.test unassigned.
        // state.test can only be assigned by that thread while the test has not
        // yet been stopped - before state.stopped is set to true. This means
        // that by the time the above wait has terminated, state.test cannot be
        // modified, and it is safe to access it without locking state. If
        // state.test is left unassigned by the test thread, the constructed
        // test object is simply discarded and no initialization or testing is
        // done. Therefore, it is safe to return a test report immediately.
        // state.cause is set by the time state.stopped is set to true, and will
        // not be modified.
        if(state.test == null)
            return new TestReport(test_class, state.cause, null);

        // The test object was constructed. clean should be called. Start a new
        // thread to call the clean method.
        new Thread(new CleanupThread(state)).start();

        // Schedule a cleanup timeout task.
        TimerTask       cleanup_timeout = new CleanupTimeoutTask(state);
        timeout_timer.schedule(cleanup_timeout, (long)timeout * 1000);

        // Wait until cleanup is stopped, either by cleanupSuccess or
        // cleanupFailure, or by the cleanup timeout task.
        synchronized(state)
        {
            while(!state.cleanup_stopped)
            {
                try
                {
                    state.wait();
                }
                catch(InterruptedException e) { }
            }
        }

        // Cancel the cleanup timeout task if it has not yet run.
        cleanup_timeout.cancel();

        // Create and return the test report. state.cause is set by the time
        // state.stopped is set to true, and state.cleanup_stop_cause is set by
        // the time state.cleanup_stopped is set to true. Not all threads are
        // guaranteed to have terminated. However, if the user wrote the clean
        // method correctly, if any threads holding system resources are still
        // running, then the clean method has timed out, causing a fatal error,
        // and so the Java runtime is about to be termiated, stopping all
        // threads.
        return new TestReport(test_class, state.cause,
                              state.cleanup_stop_cause);
    }

    /** Individual test state.

        <p>
        This class contains fields that indicate whether the test object has
        been constructed, whether the test has been stopped, whether cleanup has
        been completed, and the causes of any failures that occurred.

        <p>
        These fields cannot be placed in <code>Test</code> because test objects
        come into existence only after the test is started. In fact, it is
        possible for a test to stop before the test object has even been created
        - for example, in case timeout occurs while the constructor is still
        running.

        <p>
        It is also not advisable to place these fields directly in
        <code>Series</code>, because it is not in general possible to guarantee
        that all the threads started by one test have terminated before another
        test starts running. Because of this, it is possible that one test's
        delayed thread will call <code>Test.success</code> or
        <code>Test.failure</code> after the next test has started. In that case,
        if the fields affected by that call were in <code>Series</code> and
        shared between all tests, the delayed thread from the old test would
        terminate the next test.
     */
    static class TestState
    {
        /** Test object, if it has been created. */
        Test        test = null;
        /** Flag set if the <code>initialize</code> method has completed. The
            thread calling <code>clean</code> will not proceed until this is
            set. */
        boolean     initialize_stopped = false;

        /** Flag set if the test has stopped. This occurs after a call to
            <code>success</code> or <code>failure</code>, or due to timeout. */
        boolean     stopped = false;
        /** If the thread was stopped by a failure, this is the cause. */
        Throwable   cause = null;

        /** Flag set when cleanup has stopped. */
        boolean     cleanup_stopped = false;
        /** If cleanup failed, this is the cause. */
        FatalError  cleanup_stop_cause = null;

        /** Current task message. Regular (not cleanup) failures are wrapped in
            a <code>FailedDuringTask</code> object if they occur when this is
            not <code>null</code>. */
        String      task = null;

        /** Stops the test.

            <p>
            If this method has not yet been called for the current test, the
            <code>stopped</code> flag is set and any waiting threads are
            notified. If the method has alreay been called, calling it again has
            no effect.

            <p>
            The test termination cause is set from the parameter. If the cause
            is <code>null</code>, the test is considered to have succeeded.
            Otherwise, the test failed. <code>Test.success</code> calls this
            method with <code>null</code> as argument, and
            <code>Test.failure</code> calls this method and passes its argument.

            <p>
            If the test is being stopped due to a failure, and there is a task
            message set when the failure occurrs, then the failure is wrapped in
            a <code>FailedDuringTask</code> object which stores the task
            message.

            @param cause If the test failed, the cause of the failure, or
                         <code>null</code> if the test was successful.
         */
        synchronized void stop(Throwable cause)
        {
            if(stopped)
                return;

            if((cause != null) && (task != null))
                cause = new FailedDuringTask(task, cause);

            this.cause = cause;

            stopped = true;
            notifyAll();
        }

        /** Stops test cleanup.

            <p>
            This method may only be called after the test has stopped. Calling
            this method earlier will cause the test to be terminated with a
            fatal error.

            <p>
            If the method has not yet been successfully called, the cleanup
            process is stopped. The <code>cleanup_stopped</code> flag is set and
            any waiting threads are notified. If the method has already been
            successfully called, calling it again has no effect.

            <p>
            The <code>Test.cleanupSuccess</code> and
            <code>Test.cleanupFailure</code> call this method.

            @param cause If cleanup failed, the cause of the failure, or
                         <code>null</code> if cleanup was successful.
         */
        synchronized void stopCleanup(FatalError cause)
        {
            if(!stopped)
            {
                stop(new FatalError("cleanupSuccess or cleanupFailure method " +
                                    "called by test before cleanup is " +
                                    "started"));
                return;
            }

            if(cleanup_stopped)
                return;

            cleanup_stopped = true;
            cleanup_stop_cause = cause;
            notifyAll();
        }

        /** Sets the current task message.

            @param description Description of the current task.
         */
        synchronized void task(String description)
        {
            task = description;
        }
    }

    /** Test main thread.

        <p>
        The test main thread constructs the test object, and calls the
        <code>initialize</code> and <code>perform</code> methods.
     */
    private static class TestThread implements Runnable
    {
        /** Test class. */
        private final Class<? extends Test>     test_class;
        /** Test state object. */
        private final TestState                 state;

        /** Constructs a test thread. */
        TestThread(Class<? extends Test> test_class, TestState state)
        {
            this.test_class = test_class;
            this.state = state;
        }

        /** Runs the test thread. */
        @Override
        public void run()
        {
            // Test object constructor.
            Constructor<? extends Test>     constructor;
            // Temporary reference to the test object. After the test is
            // constructed, this method will attempt to assign the reference in
            // the state object from this reference.
            Test                            test;

            // Retrieve the test constructor.
            try
            {
                constructor = test_class.getConstructor();
            }
            catch(NoSuchMethodException e)
            {
                state.stop(new FatalError("test class does not have a public " +
                                          "no-argument constructor", e));
                return;
            }
            catch(SecurityException e)
            {
                state.stop(new FatalError("access denied to test class " +
                                          "no-argument constructor", e));
                return;
            }
            catch(Throwable t)
            {
                state.stop(new FatalError("unable to retrieve test " +
                                          "constructor", t));
                return;
            }

            // Construct the test object.
            try
            {
                test = constructor.newInstance();
            }
            catch(InvocationTargetException e)
            {
                // If the constructor throws an exception assignable to
                // TestFailed, assume it is deliberate and report the exception.
                // If it throws another exception, wrap it in a TestFailed
                // object and report.
                Throwable   cause = e.getTargetException();

                if((cause != null) && (cause instanceof TestFailed))
                    state.stop((TestFailed)cause);
                else
                {
                    state.stop(new TestFailed("test constructor threw " +
                                              "exception",
                                              e.getTargetException()));
                }

                return;
            }
            catch(Throwable t)
            {
                state.stop(new TestFailed("unable to invoke test class " +
                                          "constructor", t));
                return;
            }

            // The test object has been constructed. Take the lock on the state
            // object. If the test was stopped while the test object was being
            // constructed, return immediately and discard the test object as if
            // never constructed. In this case, clean will not be called at all.
            // Otherwise, assign the object reference and proceed to initialize
            // the test. In this case, clean will only be called after
            // initialize completes. Note that the state.initialize_stopped flag
            // is still set to false.
            synchronized(state)
            {
                if(state.stopped)
                    return;

                test.state = state;
                state.test = test;
            }

            // Initialize the test.
            try
            {
                test.initialize();
            }
            catch(Throwable t)
            {
                state.stop(t);
            }
            finally
            {
                // In all cases, whether initialization stopped normally or
                // terminated with an exception, mark the test as initialized
                // and wake up the cleaning thread, if there is one.
                synchronized(state)
                {
                    state.initialize_stopped = true;
                    state.notifyAll();
                }
            }

            // If the test has been stopped, do not call perform.
            synchronized(state)
            {
                if(state.stopped)
                    return;
            }

            // Call perform.
            try
            {
                test.perform();
            }
            catch(Throwable t)
            {
                state.stop(t);
                return;
            }

            // The test has ended successfully.
            state.stop(null);
        }
    }

    /** Timer task to time out the test main thread. */
    private static class TestTimeoutTask extends TimerTask
    {
        /** Test state. */
        private final TestState     state;

        /** Creates the timeout task. */
        TestTimeoutTask(TestState state)
        {
            this.state = state;
        }

        /** Calls <code>stop</code> indicating timeout. */
        @Override
        public void run()
        {
            state.stop(new Timeout());
        }
    }

    /** Cleanup thread.

        <p>
        The cleanup thread waits for the <code>initialize_stopped</code> flag to
        be set, and then calls <code>clean</code> on the test object.
     */
    private static class CleanupThread implements Runnable
    {
        /** Test state. */
        private final TestState     state;

        /** Creates the test cleanup thread. */
        CleanupThread(TestState state)
        {
            this.state = state;
        }

        /** Runs the cleanup thread. */
        @Override
        public void run()
        {
            // Wait for the initialize method to exit, by either return or with
            // an exception. If the initialize method takes too long to exit,
            // the cleanup thread will time out will waiting, causing a fatal
            // error.
            synchronized(state)
            {
                while(!state.initialize_stopped)
                {
                    try
                    {
                        state.wait();
                    }
                    catch(InterruptedException e) { }
                }
            }

            // Call the clean method. If that method raises any exception, this
            // is considered to be a fatal error.
            try
            {
                state.test.clean();
            }
            catch(Throwable t)
            {
                state.stopCleanup(new FatalError("clean method threw exception",
                                                 t));
            }

            // Test cleanup is assumed to have succeeded.
            state.stopCleanup(null);
        }
    }

    /** Timer task to time out the cleanup thread. */
    private static class CleanupTimeoutTask extends TimerTask
    {
        /** Test state. */
        private final TestState     state;

        /** Creates the timer task. */
        CleanupTimeoutTask(TestState state)
        {
            this.state = state;
        }

        /** Calls <code>stopCleanup</code> indicating timeout. */
        @Override
        public void run()
        {
            state.stopCleanup(new FatalError("clean method timed out"));
        }
    }

    /** Retrieves the test notice string, if there is one, or returns a default
        notice otherwise.

        <p>
        This method access the public, static, final, non-null
        <code>notice</code> field of the given class, and if it is of type
        <code>String</code>, returns its value. If the field does not exist or
        does not meet these conditions, the method returns the string
        <code>running [class name]</code>, where <code>[class name]</code> is
        the name of the test class.

        @param test_class Class for which the notice is to be retrieved.
        @return Notice string.
     */
    private static String notice(Class<? extends Test> test_class)
    {
        return staticField(test_class, String.class, "notice",
                           "running " + test_class.getSimpleName());
    }

    /** Checks that all of a test's prerequisites have been satisfied.

        <p>
        This method retrieves the value of the public, static, final, non-null
        <code>prerequisites</code> field of type <code>Class[]</code> in the
        given test class, if such a field exists. If the field does not exist,
        or does not meet these requirements, the empty array is used.

        <p>
        After obtaining the list of prerequisites in this manner, the method
        then proceeds to check whether each one is in the list of tests that
        have succeeded. If so, the method returns <code>true</code>. Otherwise,
        the method returns <code>false</code>.

        @param test_class Class for which prerequisites are to be checked.
        @param successful_tests List of tests classes that have already
                                succeeded.
        @return <code>true</code> if prerequisites for the given test are
                satisfied, <code>false</code> if not.
     */
    private static boolean ready(Class<? extends Test> test_class,
                                 List<Class<? extends Test>> successful_tests)
    {
        // Obtain the list of prerequisites.
        @SuppressWarnings("unchecked")
        Class<? extends Test>[]     prerequisites =
            staticField(test_class, Class[].class, "prerequisites",
                        new Class[] { });

        // Check the prerequisites.
        for(Class<? extends Test> prerequisite : Arrays.asList(prerequisites))
        {
            if(!successful_tests.contains(prerequisite))
                return false;
        }

        return true;
    }

    /** Retrieves the value of a static field in a class.

        <p>
        The field must be public, static, and final. It must have a type that is
        assignable to the given type, its name must match the given name, and it
        must not be <code>null</code>. If the given class has no such field, the
        method returns the given default value.

        @param c Class from which the field is to be retrieved.
        @param field_type Field type. The actual field type may be any type that
                          is assignable to this t ype.
        @param name Field name.
        @param default_value Default value to be used if the actual field value
                             cannot be retrieved.
        @return The value of the field, or the default value if a field matching
                the requirements cannot be found in the class.
     */
    private static <T> T staticField(Class<?> c, Class<T> field_type,
                                     String name, T default_value)
    {
        // Value to be returned. This reference will be reassigned if field
        // value retrieval succeeds.
        T           value = default_value;
        Field       field;

        // If the following operations raise an exception, the value variable
        // will not be reassigned, so the default value will be returned.
        // Failure of some checks results in a descriptive exception which is
        // ignored. The code is written this way to allow it to easily later be
        // converted to a form where the exceptions are propagated.
        try
        {
            // Find the field in the class.
            field = c.getField(name);

            // Check that the field has a correct type.
            if(!field_type.isAssignableFrom(field.getType()))
            {
                throw new NoSuchElementException("no field is present in " +
                                                 c + " with type " +
                                                 field_type + " and name " +
                                                 name);
            }

            // Check that the field is static and final.
            if(!Modifier.isStatic(field.getModifiers()))
            {
                throw new NoSuchElementException(name + " is not static in " +
                                                 c);
            }

            if(!Modifier.isFinal(field.getModifiers()))
            {
                throw new NoSuchElementException(name + " is not final in " +
                                                 c);
            }

            // Retrieve the value of the field.
            @SuppressWarnings("unchecked")
            T       unchecked_value = (T)field.get(null);

            // Check that the value is not null.
            if(unchecked_value == null)
                throw new NullPointerException(name + " in " + c + " is null");

            // If all the above checks succeed, return the retrieved value of
            // the field.
            value = unchecked_value;
        }
        catch(Throwable t) { }

        return value;
    }
}
