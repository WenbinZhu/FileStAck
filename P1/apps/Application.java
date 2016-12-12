package apps;

/** Base class of filesystem applications.

    <p>
    This base class allows applications to be run from a monolithic Java
    program. Each filesystem application has this class as a common base type.
    This permits application objects to be easily inserted into collections and
    started programmatically.
 */
abstract class Application
{
    /** Exit code returned to the system on success. */
    protected static final int      EXIT_SUCCESS = 0;
    /** Exit code returned to the system on failure. */
    protected static final int      EXIT_FAILURE = 2;

    /** Runs the application.

        @param arguments Command line arguments.
     */
    abstract void run(String[] arguments);
}
