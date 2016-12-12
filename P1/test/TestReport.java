package test;

import java.io.*;

/** Individual test report.

    <p>
    This class contains fields and convenience methods related to the result of
    running a single test. It is used internally by <code>Series</code> to pass
    test results. For failed tests, it is returned in <code>SeriesReport</code>
    objects.
 */
class TestReport implements Serializable
{
    /** Test class. */
    private final Class<? extends Test>     test_class;
    /** Reason for test failure, or <code>null</code> if the test was
        successful. */
    private final Throwable                 stop_cause;
    /** Task message when the test failed, or <code>null</code> if there was no
        task message, or the test was successful. */
    private final String                    task;
    /** Reason for test cleanup failure, or <code>null</code> if the test was
        cleaned up successfully. */
    private final FatalError                cleanup_stop_cause;

    /** Creates a <code>TestReport</code> object.

        <p>
        If <code>stop_cause</code> is wrapped in a <code>FailedDuringTask</code>
        object, it is unwrapped, and the task noted in the <code>task</code>
        field.
     */
    TestReport(Class<? extends Test> test_class, Throwable stop_cause,
               FatalError cleanup_stop_cause)
    {
        this.test_class = test_class;
        this.cleanup_stop_cause = cleanup_stop_cause;

        // Unwrap the stop_cause object, if necessary.
        if((stop_cause != null) && (stop_cause instanceof FailedDuringTask))
        {
            this.stop_cause = stop_cause.getCause();
            this.task = stop_cause.getMessage();
        }
        else
        {
            this.stop_cause = stop_cause;
            this.task = null;
        }
    }

    /** Returns <code>true</code> if and only if the test was successful: if
        both the test stop cause and the cleanup stop cause are
        <code>null</code>.
     */
    boolean successful()
    {
        return (stop_cause == null) && (cleanup_stop_cause == null);
    }

    /** Returns <code>true</code> if and only if the test stopped with a fatal
        error, or cleanup stopped with any error (all cleanup erros are fatal).
     */
    boolean fatal()
    {
        if(cleanup_stop_cause != null)
            return true;

        if(stop_cause == null)
            return false;

        return (stop_cause instanceof FatalError);
    }

    /** Prints a report for a failed test.

        <p>
        The test name is printed. If the main test failed, then the reason for
        the failure is printed. The task being performed during the failure is
        shown, if there was one. If the failure was not a timeout, the stack
        trace is shown as well. If the cleanup task failed, a description of the
        error and a stack trace are printed.

        @param stream Stream to receive the formatted test report.
     */
    void print(PrintStream stream)
    {
        // Do nothing if the test was successful.
        if(successful())
            return;

        // Print report header.
        stream.println("failed test:    " + test_class.getSimpleName());

        // If the main test failed, print the reason, and optionally the task
        // message and a stack trace.
        if(stop_cause != null)
        {
            stream.println("reason:         " + stop_cause.getMessage());

            if(task != null)
                stream.println("task:           " + task);

            if(!(stop_cause instanceof Timeout))
            {
                stream.println("stack trace:    ");
                stop_cause.printStackTrace(stream);
            }
        }

        // If cleanup failed, print the reason and a stack trace.
        if(cleanup_stop_cause != null)
        {
            stream.println("cleanup failed: " +
                           cleanup_stop_cause.getMessage());
            cleanup_stop_cause.printStackTrace(stream);
        }
    }
}
