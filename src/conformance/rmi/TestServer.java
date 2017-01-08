package conformance.rmi;

import rmi.*;
import java.io.FileNotFoundException;

/** Simple implementation of <code>TestInterface</code>.

    <p>
    This class is used in multiple tests.
 */
class TestServer implements TestInterface
{
    /** The sleeping thread does not return until this becomes
        <code>false</code>. */
    private boolean     sleeping = true;
    /** If <code>true</code>, the next thread to call <code>rendezvous</code>
        should wake all sleeping threads. */
    private boolean     wake = false;

    // Methods documented in TestInterface.java.
    @Override
    public Object method(boolean throw_exception)
        throws RMIException, FileNotFoundException
    {
        if(throw_exception)
            throw new FileNotFoundException();
        else
            return null;
    }

    @Override
    public synchronized void rendezvous() throws RMIException
    {
        // If wake is false, this thread should go to sleep. If it is true,
        // this thread should wake the sleeping thread.
        if(!wake)
        {
            wake = true;

            while(sleeping)
            {
                try
                {
                    wait();
                }
                catch(InterruptedException e) { }
            }
        }
        else
        {
            sleeping = false;

            notifyAll();
        }
    }

    /** Wakes all sleeping receiving threads. */
    public synchronized void wake()
    {
        sleeping = false;

        notifyAll();
    }
}
