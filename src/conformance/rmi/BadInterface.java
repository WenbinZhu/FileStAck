package conformance.rmi;

/** Non-remote interface that RMI constructors should reject.

    <p>
    This interface is used in multiple tests.
 */
public interface BadInterface
{
    /** Causes the interface to be rejected for use in RMI.

        <p>
        This public method is not declared as throwing
        <code>RMIException</code>, and therefore this interface is not
        considered to be a remote interface.
     */
    public Object method(int argument) throws java.io.FileNotFoundException;
}
