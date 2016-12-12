package storage;

import java.io.*;

import common.*;
import rmi.RMIException;

/** Storage server client interface.

    <p>
    All methods in this interface may raise {@link NullPointerException} if
    passed <code>null</code> for arguments or {@link SecurityException} if the
    security manager on the server does not allow an operation.
 */
public interface Storage
{
    /** Returns the length of a file, in bytes.

        @param file Path to the file.
        @return The length of the file.
        @throws FileNotFoundException If the file cannot be found or the path
                                      refers to a directory.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public long size(Path file) throws RMIException, FileNotFoundException;

    /** Reads a sequence of bytes from a file.

        @param file Path to the file.
        @param offset Offset into the file to the beginning of the sequence.
        @param length The number of bytes to be read.
        @return An array containing the bytes read. If the call succeeds, the
                number of bytes read is equal to the number of bytes requested.
        @throws IndexOutOfBoundsException If the sequence specified by
                                          <code>offset</code> and
                                          <code>length</code> is outside the
                                          bounds of the file, or if
                                          <code>length</code> is negative.
        @throws FileNotFoundException If the file cannot be found or the path
                                      refers to a directory.
        @throws IOException If the file read cannot be completed on the server.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public byte[] read(Path file, long offset, int length)
        throws RMIException, FileNotFoundException, IOException;

    /** Writes bytes to a file.

        @param file Path to the file.
        @param offset Offset into the file where data is to be written.
        @param data Array of bytes to be written.
        @throws IndexOutOfBoundsException If <code>offset</code> is negative.
        @throws FileNotFoundException If the file cannot be found or the path
                                      refers to a directory.
        @throws IOException If the file write cannot be completed on the server.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    public void write(Path file, long offset, byte[] data)
        throws RMIException, FileNotFoundException, IOException;
}
