package apps;

import java.util.*;

/** Base class of client applications.

    <p>
    This base class provides its own <code>run</code> method. Particular
    applications are implemented by overriding the <code>coreLogic</code>
    method of this class. The <code>run</code> method in this class wraps the
    main application logic in a <code>try...catch</code> statement. The
    application logic need not concern itself with error codes or printing error
    messages. If the <code>coreLogic</code> method throws an exception, an error
    message is printed to standard error, and the exit code is non-zero.

    <p>
    This class also provides an exception aggregation mechanism. If an
    application repeats certain work on each of several arguments, instead of
    the first exception terminating the whole program, it is possible to
    accumulate exceptions from each argument, and display all of them only after
    all arguments have been processed. This is done using the
    <code>report</code> methods. An application which wishes to process multiple
    arguments in this way should call <code>report</code> with no arguments
    after each argument that is processed successfully, and <code>report</code>
    with the exception as an argument after each argument that causes a failure
    during processing.

    <p>
    Calling <code>fatal</code> during processing causes the application to quit
    at the next call to <code>report</code>. Fatal errors are those in which
    resources which must be released to continue running the application safely
    cannot be released.
 */
abstract class ClientApplication extends Application
{
    /** Fatal error message, if a fatal error has occurred. */
    private String                          fatal_error_message = null;
    /** Aggregated list of application error messages. */
    private LinkedList<ApplicationFailure>  failures =
        new LinkedList<ApplicationFailure>();

    /** Called to execute the main body of the application.

        @param arguments Command line arguments passed to the application.
        @throws ApplicationFailure When an error occurs. If this exception is
                                   thrown, its message string is printed to the
                                   standard error stream, and the application's
                                   exit status is non-zero.
        @throws Throwable If the application failed to handle an error by
                          responding to it, or by translating it to
                          <code>ApplicationFailure</code>.
     */
    protected abstract void coreLogic(String[] arguments)
        throws ApplicationFailure;

    /** Wraps <code>coreLogic</code> in an exception handler.

        @param arguments Command line arguments.
     */
    @Override
    public void run(String[] arguments)
    {
        try
        {
            coreLogic(arguments);
        }
        catch(ApplicationFailure e)
        {
            report(e);
        }
        catch(Throwable t)
        {
            System.err.println("exception caught at top level: " + t);
            t.printStackTrace(System.err);
            System.exit(EXIT_FAILURE);
        }

        printErrorsAndExit();
    }

    /** Reports that an argument has been processed successfully, for an
        application that processes multiple arguments.

        <p>
        The primary purpose of this method is to act as a checkpoint. Fatal
        errors are sometimes the result of code that is run in
        <code>finally</code> blocks. Such code typically avoids raising
        exceptions when it fails, in order to avoid masking a primary exception.
        Thus, fatal errors are signalled instead by calling the
        <code>fatal</code> method. A method which called <code>fatal</code>, but
        had no other errors, will appear to have returned successfully.
        <code>report</code> must be called after such a method is run to ensure
        that the fatal error causes termination of the program.
     */
    protected void report()
    {
        if(fatal_error_message != null)
            printErrorsAndExit();
    }

    /** Reports that an argument has been processed, but the processing has
        caused a failure.

        <p>
        The failure is added to a list of application errors, and will be
        printed on application exit. If a fatal error has been signalled, the
        application exits immediately.

        @param failure Application failure.
     */
    protected void report(ApplicationFailure failure)
    {
        failures.add(failure);
        report();
    }

    /** Prints all errors and terminates the process.

        <p>
        The exit status is zero if there are no errors, and non-zero otherwise.
     */
    private void printErrorsAndExit()
    {
        int     exit_status = EXIT_SUCCESS;

        for(ApplicationFailure failure : failures)
        {
            System.err.println(failure.getMessage());
            exit_status = EXIT_FAILURE;
        }

        if(fatal_error_message != null)
        {
            System.err.println("fatal error: " + fatal_error_message);
            exit_status = EXIT_FAILURE;
        }

        System.exit(exit_status);
    }

    /** Indicates that a fatal error has occurred.

        @param message Description of the error.
     */
    protected void fatal(String message)
    {
        fatal_error_message = message;
    }
}
