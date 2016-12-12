/** Naming (metadata) server.

    <p>
    The filesystem <em>naming server</em> maintains the common directory tree.
    Separate <em>storage servers</em> provide storage space for files.

    <p>
    Some filesystem operations, such as directory creation and listing, are
    handled entirely by the naming server. Others, such as file reading and
    writing, are handled by the storage servers. The latter operations require
    the client to first contact the naming server to obtain an RMI stub for the
    storage server that stores the file in question.

    <p>
    Consistency between the naming and storage servers is strictly maintained.
    Every file present on a storage server is listed on the naming server. File
    creation and deletion is synchronized between the naming server and the
    storage servers: for instance, the file creation operation does not complete
    on the naming server until the storage server has confirmed it has created
    an empty file of the same name.

    <p>
    Upon startup, the storage servers inform the naming server of which files
    they are initially able to serve, and provide RMI stubs for accessing them.
    This process is <em>registration</em>. The naming server then requests that
    the storage server remove duplicate files that are already present in the
    naming server's directory tree.

    <p>
    The naming server provides two interfaces: a <em>service interface</em>
    through which clients can perform naming server operations and obtain stubs
    for storage servers, and a <em>registration interface</em>, through which
    storage servers notify the naming server of their existence. Both interfaces
    are RMI skeletons running at well-known ports.
 */
package naming;
