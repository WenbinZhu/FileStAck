package conformance.rmi;

import rmi.RMIException;
import java.io.FileNotFoundException;

/** Simple interface for an RMI server.

    <p>
    This interface is used in multiple tests.
 */
public interface TestInterface
{
    /** Tests transmission of arguments and returning of results.

        @param throw_exception If <code>true</code>, this method throws
                               <code>FileNotFoundException</code>. Otherwise, it
                               returns <code>null</code>.
        @return <code>null</code>.
        @throws FileNotFoundException If the argument is <code>true</code>.
        @throws RMIException If the call cannot be complete due to a network
                             error.
     */
    public Object method(boolean throw_exception)
        throws RMIException, FileNotFoundException;

    /** Permits two threads to rendzevous with control inside the server.

        <p>
        The first thread to call this method blocks until a second method wakes
        it.

        @throws RMIException If the call cannot be complete due to a network
                             error.
     */
    public void rendezvous() throws RMIException;
}
