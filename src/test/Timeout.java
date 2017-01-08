package test;

/** Indicates that a test has failed due to timeout.

    <p>
    This class is distinguished from <code>TestFailed</code> to permit reporting
    code to be able to reliably distinguish between regular failures and
    timeouts, and to avoid printing stack traces for timeouts.
 */
class Timeout extends TestFailed
{
    /** Constructs a <code>Timeout</code> object. */
    Timeout()
    {
        super("test timed out");
    }
}
