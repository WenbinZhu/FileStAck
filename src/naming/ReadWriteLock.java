package naming;

/** Each path has its own ReadWriteLock object
 */
public class ReadWriteLock {
    private int readingNum = 0;
    private int writeRequest = 0;
    private boolean isWriting = false;

    /** Get a shared lock on the node, or blocked if lock cannot be obtained
     */
    public synchronized void lockShared() throws InterruptedException
    {
        while (writeRequest > 0 || isWriting)
            wait();

        readingNum++;
    }

    /** Get an exclusive lock on the node, or blocked if lock cannot be obtained
     */
    public synchronized void lockExclusive() throws InterruptedException
    {
        writeRequest++;

        while (readingNum > 0 || isWriting)
            wait();

        writeRequest--;
        isWriting = true;
    }

    /** Release the shared lock on the node
     */
    public synchronized void unlockShared()
    {
        readingNum--;

        if (readingNum < 0)
            throw new IllegalStateException("Server error, number of thread reading less than 0");

        notifyAll();
    }

    /** Release the exclusive lock on the node
     */
    public synchronized void unlockExclusive()
    {
        isWriting = false;
        notifyAll();
    }
}
