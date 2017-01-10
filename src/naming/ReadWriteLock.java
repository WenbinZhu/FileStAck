package naming;

public class ReadWriteLock {
    private int readingNum = 0;
    private int writeRequest = 0;
    private boolean isWriting = false;

    public synchronized void lockShared() throws InterruptedException
    {
        while (writeRequest > 0 || isWriting)
            wait();

        readingNum++;
    }

    public synchronized void lockExclusive() throws InterruptedException
    {
        writeRequest++;

        while (readingNum > 0 || isWriting)
            wait();

        writeRequest--;
        isWriting = true;
    }

    public synchronized void unlockShared()
    {
        readingNum--;

        if (readingNum < 0)
            throw new IllegalStateException("Server error, number of thread reading less than 0");

        notifyAll();
    }

    public synchronized void unlockExclusive()
    {
        isWriting = false;
        notifyAll();
    }
}
