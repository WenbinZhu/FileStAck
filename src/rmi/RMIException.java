package rmi;

/** RMI exceptions. */
public class RMIException extends Exception
{
    /** Creates an <code>RMIException</code> with the given message string. */
    public RMIException(String message)
    {
        super(message);
    }

    /** Creates an <code>RMIException</code> with a message string and the given
        cause. */
    public RMIException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /** Creates an <code>RMIException</code> from the given cause. */
    public RMIException(Throwable cause)
    {
        super(cause);
    }
}
