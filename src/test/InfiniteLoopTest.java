package test;

/** Starts an infinite loop and runs until timeout. */
public class InfiniteLoopTest extends Test
{
    /** Test notice. */
    public static final String  notice =
        "checking timeout with an infinite loop (should fail)";

    /** Flag used to force the main (looping) thread to terminate. */
    private boolean     running = true;

    /** Performs the test.

        <p>
        This method enters an infinite loop, which may only be terminated by
        <code>clean</code> upon termination of the test by timeout.
     */
    @Override
    protected synchronized void perform()
    {
        while(running)
        {
            try
            {
                wait();
            }
            catch(InterruptedException e) { }
        }
    }

    /** Terminates the infinite loop. */
    @Override
    protected synchronized void clean()
    {
        running = false;
        notifyAll();
    }
}
