package test;

/** Base class of tests.

    <p>
    Tests are created by subclassing <code>Test</code> and overriding the
    <code>perform</code> method. The test author may optionally also override
    the <code>clean</code> and <code>initialize</code> methods. Tests should
    have a public no-argument constructor (such as the default constructor).
    Optionally, tests may include two static fields, <code>notice</code> and
    <code>prerequisites</code>, which are used by the testing library for
    printing progress, and for deciding the order in which tests are run,
    respectively. When a test has finished, it should call <code>success</code>
    or <code>failure</code> to indicate the outcome. A minimal test class
    therefore is:
    <pre>
    public class MinimalTest extends test.Test
    {
        protected void perform()
        {
            success();
        }
    }
    </pre>
    A more complex example:
    <pre>
    public class ComplexTest extends test.Test
    {
        public static final String  notice = "checking property X";
        public static final Class[] prerequisites =
            new Class[] {OtherTest.class, AnotherTest.class};

        // Declarations of members go here, including system resources.

        protected void perform()
        {
            // Test body here. The test may start threads.

            success();
        }

        protected void initialize()
        {
            // Initialize system resources.
        }

        protected void clean()
        {
            // Clean up system resources and terminate test threads.
        }
    }
    </pre>
    Exceptions thrown from <code>initialize</code> and <code>perform</code> are
    equivalent to calls to <code>failure</code>. Exceptions thrown from
    <code>clean</code> are equivalent to calls to <code>cleanupFailure</code>.
    Returning from <code>perform</code> or <code>clean</code> is equivalent to
    calling <code>success</code> and <code>cleanupSuccess</code>, respectively.

    <p>
    Tests are run as part of a <code>test series</code>. This is captured by
    <code>{@link Series}</code> objects.

    <p>
    The <code>perform</code> method runs the bulk of the test. This method, or
    a method in another thread, should eventually call <code>success</code> or
    <code>failure</code> to indicate the result of the test. If neither the
    <code>perform</code> method nor a method run by one of the threads started
    by <code>perform</code> calls either <code>success</code> or
    <code>failure</code> within a certain time period, the test will be
    terminated by a timer started by the testing library. The timer will itself
    call <code>failure</code>.

    <p>
    The first call to <code>success</code> or <code>failure</code> determines
    the outcome of the test. Subsequent calls to either method have no effect.

    <p>
    After the test is stopped, whether by an explicit call to
    <code>success</code> or <code>failure</code>, or due to timeout, the
    <code>clean</code> method is called. The purpose of the <code>clean</code>
    method is to stop test threads and to release system resources acquired by
    the test. The <code>clean</code> method must likewise eventually cause
    either <code>cleanupSuccess</code> or <code>cleanupFailure</code> to be
    called. If the <code>clean</code> method fails to do so, the cleanup
    process will time out, and will be considered to have failed.

    <p>
    Because of the test timer, and because <code>perform</code> might start many
    threads, a test may, in principle, terminate at any point after
    <code>perform</code> is started, including while it is still running, and
    therefore <code>clean</code> may start at any time while
    <code>perform</code> is still running. The intended coding style for
    <code>perform</code> and <code>clean</code> then is that <code>clean</code>
    will set references to system objects to <code>null</code>, so that
    <code>perform</code> will receive <code>NullPointerException</code>s when it
    attempts to use them. <code>clean</code> should also close sockets, streams,
    and other I/O objects that <code>perform</code> or other test threads may be
    blocked on, to ensure their speedy termination. The exceptions caused by
    these disruptions will lead to calls to <code>failure</code> by
    <code>perform</code> and the other threads. These calls, however, will be
    safely ignored, as the outcome of the test will already have been determined
    by the time <code>clean</code> is started.

    <p>
    This coding style complicates initialization. If a system object reference
    is being initialized inside <code>perform</code>, there is a race condition
    with <code>clean</code>: <code>clean</code> may run first, find that the
    reference is still <code>null</code>, and do nothing to release the object.
    <code>perform</code>, which has not yet been terminated by an exception,
    may then set the reference to a new system object, and continue using it. To
    prevent this scenario, a third method <code>initialize</code> is provided to
    the test author. <code>initialize</code> is as <code>perform</code>, the
    difference being that it is guaranteed that <code>clean</code> will only be
    called after <code>initialize</code> has completed. It is therefore safe to
    initialize system objects within <code>initialize</code> without taking
    additional safety precautions.

    <p>
    Tests should perform no system object initialization within their
    constructors. In fact, the intent is that the default constructor be used
    for the majority of tests. A derived class should generally not have an
    explicit written constructor. The reason is that if initialization fails
    while partially complete in <code>initialize</code>, the test author may
    simply call <code>failure</code> (or allow an exception to escape), and rely
    on the subsequent call to <code>clean</code> to clean up the objects that
    were created. If, instead, initialization fails while partially complete in
    the constructor, then the testing library cannot call <code>clean</code> -
    there is no object yet to call <code>clean</code> on.

    <p>
    The testing library expects every test to have a public no-argument
    constructor. The default constructor suffices for this purpose. If the
    author wishes to write constructors, one must be provided which is public
    and takes no arguments. Constructors written by the author may throw
    exceptions, and these will be caught by the testing library. Generally, the
    rules for constructor exceptions are the same as for exceptions thrown from
    <code>initialize</code> and <code>perform</code>.

    <p>
    Failure of a test during the <code>initialize</code> or <code>perform</code>
    methods is considered to be regular test failure, and the testing library
    may try to run more tests. Failure of a test during cleanup is considered a
    fatal error, and the testing library will not attempt any more tests. This
    is because the test that failed to clean up after itself may be holding
    exclusive resources, which may cause subsequent tests to fail when they are
    unable to acquire them.

    <p>
    As a side effect, timeout during initialization will likely lead to a fatal
    error. This is because the <code>clean</code> method does not attempt to
    terminate <code>initialize</code>, waiting instead for it to complete. If
    the <code>initialize</code> method does not stop on its own soon after it
    times out, the <code>clean</code> method will also time out and fail.

    <p>
    A test may make assumptions about the correctness of certain features, which
    are tested by another test. In this case, it is highly desirable for the
    other test to be run first. The test author may indicate this dependency to
    the testing library by creating a public, static, final field in the test
    class with the name <code>prerequisites</code>, as shown in the example
    above.

    <p>
    Another static field, <code>notice</code>, may be used to give the message
    that should be printed when the test is run by the <code>Series.run</code>
    method, if printing is turned on.

    <p>
    Each test object has an optional task message, settable by the author. The
    task message describes the current activity of the test. If a test failure
    occurs while there is a task message set, the exception representing the
    failure will include the message. Later, when the failure report is printed,
    the message will be printed as part of the description of the failure. This
    is especially useful when a test performs long-running operations which may
    time out. If such an operation times out with no task message set, the
    report printed will simply state that there was a timeout, leaving the
    reader with no hint of what the test was doing when the timeout occurred.
    In this case, the test author should set the task message before starting
    the operation, and clear it after the operation completes.

    <p>
    Calls to <code>failure</code> by the testing library itself, described
    above, may instead bypass <code>failure</code> and directly call a
    library-private implementation method. It is therefore not, in general,
    possible to override <code>failure</code> and expect to capture every
    attempt to stop the test. In particular, the timeout threads deliberately do
    not call <code>failure</code>, and instead call the underlying
    implementation method.
 */
public abstract class Test
{
    /** Test state reference. See {@link Series.TestState}.

        <p>
        This reference is set by a thread in the <code>Series</code> object
        immediately after the test object is constructor, but is not available
        while the constructor is still running.
     */
    Series.TestState    state = null;

    /** Called to initialize the test object.

        <p>
        This method is called before <code>perform</code>. It is guaranteed that
        the <code>clean</code> method will not be called until
        <code>initialize</code> has exited. The <code>initialize</code> method,
        or threads started by <code>initialize</code>, may call
        <code>success</code> or <code>failure</code> to terminate the test. If
        this occurs, <code>clean</code> will be called afterwards. The test may
        also be terminated by timeout. Therefore, <code>clean</code> should be
        prepared to handle a partially-initialized test object. Code in
        <code>clean</code> and <code>initialize</code> should be written in a
        way that allows partially-initialized objects to be reliably cleaned up.

        <p>
        Throwing any exception from <code>initialize</code> results in a call to
        <code>failure</code>. If the exception thrown is assignable to
        <code>TestFailed</code>, the <code>reason</code> argument given to
        <code>failure</code> is the exception. Otherwise, the exception is
        wrapped in a <code>TestFailed</code> object. Test authors overriding
        <code>initialize</code> are strongly encouraged to mark the subclass
        implementation as throwing only <code>TestFailed</code>, and throw only
        that exception.

        @throws Throwable Upon test failure. The <code>Throwable</code> is
                          wrapped in a <code>TestFailed</code> object, if it is
                          not already one, and passed to <code>failure</code>.
     */
    protected void initialize() throws Throwable
    {
    }

    /** Called to perform the test.

        <p>
        This method is called after <code>initialize</code>. The
        <code>clean</code> method may be called in another thread while this
        method is running. <code>perform</code> should therefore be ready to
        handle the effects of the actions done by <code>clean</code>. Generally,
        <code>clean</code> will set object references to <code>null</code> and
        close I/O streams that <code>perform</code> or threads started by
        <code>perform</code> are using. It is acceptable for
        <code>perform</code> to throw exceptions in response to these actions.
        These exception will not affect the outcome of the test, as the outcome
        has been determined by the time <code>clean</code> has started running.
        The purpose of these exceptions is to terminate <code>perform</code> and
        any threads started by it as quickly as possible. The test author must
        take care, however, that the exceptions do not adversely affect state
        internal to the test itself, and do not damage system objects.

        <p>
        <code>perform</code> or one of the threads started by it should call
        <code>success</code> or <code>failure</code> once the outcome of the
        test is known. <code>perform</code> is run within its own thread by the
        testing library. There is an additional parallel thread started by the
        testing library, which will call <code>failure</code> after a timeout
        interval if <code>perform</code> fails to terminate the test.

        <p>
        Throwing any exception from <code>perform</code> results in a call to
        <code>failure</code>. Remarks regarding exceptions thrown from
        <code>perform</code> are the same as for exceptions thrown from
        <code>initialize</code>. In particular, the test author is strongly
        encouraged to mark subclass implementations as throwing only
        <code>TestFailed</code>.

        <p>
        Returning from <code>perform</code> causes <code>success</code> to be
        called. If this is not desired, the test author should have
        <code>perform</code> wait until some condition internal to the test is
        satisfied.

        @throws Throwable Upon test failure. The <code>Throwable</code> is
                          wrapped in a <code>TestFailed</code> object, if it is
                          not already one, and passed to <code>failure</code>.
     */
    protected abstract void perform() throws Throwable;

    /** Called to terminate test threads and release system resources.

        <p>
        This method is called after <code>success</code> or <code>failure</code>
        has been called, and the test has been terminated. Note that in addition
        to the code written by the test author, there is also a timeout thread
        in the testing library which will call <code>failure</code> after the
        timeout interval. <code>clean</code> is guaranteed to be called only
        after <code>initialize</code> has completed, but may be called during
        <code>perform</code>.

        <p>
        <code>clean</code>, or another thread started by it, should call
        <code>cleanupSuccess</code> or <code>cleanupFailure</code>. Throwing any
        exception from <code>clean</code> is equivalent to a call to
        <code>cleanupFailure</code>. A thread is started in parallel with
        <code>clean</code>. If the cleanup process fails to terminate before a
        timeout interval, this other thread calls <code>cleanupFailure</code>.

        <p>
        Cleanup failure is a fatal error. If a test is unable to release system
        resources, subsequent tests, which may demand them, will be unable to
        acquire them. This can lead to potentially misleading test failure
        messages. To prevent this, the testing library does not run any more
        tests after a test fails in the cleanup process.

        <p>
        Returning from <code>clean</code> is considered to be an implicit call
        to <code>cleanupSuccess</code>.

        @throws Throwable If cleanup cannot be completed. This results in a
                          fatal error.
     */
    protected void clean() throws Throwable
    {
    }

    /** Terminates the test as successful.

        <p>
        This method should be called from <code>initialize</code> or
        <code>perform</code>, or from one of the threads started by them. If
        <code>success</code> or <code>failure</code> have not yet been called,
        calling this method results in a later call to <code>clean</code>.
        Otherwise, calling this method has no effect.

        @throws IllegalStateException If the test object is still being
                                      constructed, and therefore neither
                                      <code>initialize</code> not
                                      <code>perform</code> have been called.
     */
    public void success()
    {
        if(state == null)
        {
            throw new IllegalStateException("test success method called " +
                                            "while test is being constructed");
        }

        state.stop(null);
    }

    /** Terminates the test as a failure.

        <p>
        This method should be called from <code>initialize</code> or
        <code>perform</code>, or from one of the threads started by them. If
        <code>success</code> or <code>failure</code> have not yet been called,
        calling this method results in a later call to <code>clean</code>.
        Otherwise, calling this method has no effect.

        @param reason Reason for the test failure. The test author is strongly
                      encouraged to make the reason a descriptive TestFailed
                      object which explains the situation that lead to the
                      underlying exception, if there is one. However, for
                      convenience, it is possible to call <code>failure</code>
                      with any <code>Throwable</code> as the argument.
        @throws IllegalStateException If the test object is still being
                                      constructed, and therefore neither
                                      <code>initialize</code> not
                                      <code>perform</code> have been called.
     */
    public void failure(Throwable reason)
    {
        if(state == null)
        {
            throw new IllegalStateException("test failure method called " +
                                            "while test is being constructed");
        }

        state.stop(reason);
    }

    /** Indicates that cleanup has been successfully completed.

        <p>
        This method should be called from <code>clean</code> or from one of the
        threads started by it. If <code>cleanupSuccess</code> or
        <code>cleanupFailure</code> have not yet been called, this terminates
        the cleanup process. Otherwise, calling this method has no effect.

        <p>
        Calling this method before <code>clean</code> has been called results in
        a call to <code>failure</code>, and is a fatal error.

        @throws IllegalStateException If the test object is still being
                                      constructed, and therefore
                                      <code>clean</code> has not been called.
     */
    public void cleanupSuccess()
    {
        if(state == null)
        {
            throw new IllegalStateException("test cleanupSuccess method " +
                                            "called while test is being " +
                                            "constructed");
        }

        state.stopCleanup(null);
    }

    /** Indicates that cleanup has failed.

        <p>
        This method should be called from <code>clean</code> or from one of the
        threads started by it. If <code>cleanupSuccess</code> or
        <code>cleanupFailure</code> have not yet been called, this terminates
        the cleanup process. Otherwise, calling this method has no effect.

        <p>
        Calling this method before <code>clean</code> has been called results in
        a call to <code>failure</code>, and is a fatal error.

        @param reason Reason for the cleanup failure.
        @throws IllegalStateException If the test object is still being
                                      constructed, and therefore
                                      <code>clean</code> has not been called.
     */
    public void cleanupFailure(Throwable reason)
    {
        if(state == null)
        {
            throw new IllegalStateException("test cleanupFailure method " +
                                            "called while test is being " +
                                            "constructed");
        }

        state.stopCleanup(new FatalError("cleanup failed with explicit call " +
                                         "to cleanupFailure", reason));
    }

    /** Sets the test task message.

        <p>
        Any existing task message is replaced.

        @param description Description of the current task.
        @throws IllegalStateException If the test object is still being
                                      constructed.
     */
    public void task(String description)
    {
        if(state == null)
        {
            throw new IllegalStateException("test task method called while " +
                                            "test is being constructed");
        }

        state.task(description);
    }

    /** Clears the task message.

        @throws IllegalStateException If the test object is still being
                                      constructed.
     */
    public void task()
    {
        if(state == null)
        {
            throw new IllegalStateException("test task method called while " +
                                            "test is being constructed");
        }

        state.task(null);
    }
}
