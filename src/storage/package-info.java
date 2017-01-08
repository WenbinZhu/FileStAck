/** Storage server.

    <p>
    Storage servers provide actual storage space for files in the filesystem.
    Storage is backed by the underlying local filesystem on the machine on which
    each server is running.

    <p>
    Upon startup, the storage server recursively lists a certain directory on
    its local filesystem. It transmits the resulting directory tree to the
    naming server. Previously unavailable files are then listed by the naming
    server.

    <p>
    The client is not initially directly aware of the storage servers. Instead,
    the client obtains RMI stubs for storage servers from the naming server.
    Storage servers provide these stubs to the naming server on startup. For
    this reason, it is immaterial which port each storage server is running on,
    or its exact network address, so long as it is reachable by the client.
 */
package storage;
