package apps;

/** Signifies an application failure, and that the application should exit with
    status <code>EXIT_FAILURE</code> (<code>2</code>). */
class ApplicationFailure extends Exception
{
    /** Creates an <code>ApplicationFailure</code> and sets the message string.

        <p>
        The string is later printed before the application exits.
     */
    ApplicationFailure(String message)
    {
        super(message);
    }
}
