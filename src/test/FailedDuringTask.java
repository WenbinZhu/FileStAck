package test;

/** Represents a test failure that occurred while the test was performing a
    particular task. */
class FailedDuringTask extends Exception
{
    /** Constructs a <code>FailedDuringTask</code> object with the given task
        message and underlying failure. */
    FailedDuringTask(String task, Throwable cause)
    {
        super(task, cause);
    }
}
