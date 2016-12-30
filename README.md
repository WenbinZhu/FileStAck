# FileStAck (Remote File Storage and Access Kit)
### A Distributed File System

<br/>
To run the application, build dfs.jar. Then, from a bash shell:

```
. dfs cd directory
    Set the current directory and current host. Note that you must source the
    script (using the . or source commands) instead of simply running it,
    because the cd command needs to update the DFSHOST and DFSCWD environment
    variables of your shell. With a hostname and a current directory defined,
    you can write paths in the following formats:

        hostname:absolute-path      gives both the hostname and the full path
        hostname:relative-path      gives the hostname, but the path is relative
                                    to the last path set by cd
        :absolute-path              gives an absolute path on the last host set
                                    by cd
        :relative-path              gives a path relative to the last path set
                                    by cd, on the last host set by cd
        absolute-path               equivalent to :absolute-path
        relative-path               equivalent to :relative-path

    For example, the sequence

        . dfs cd 127.0.0.1:/directory
        . dfs ls subdirectory

    has the same effect as the single command

        . dfs ls 127.0.0.1:/directory/subdirectory

    The cd command does not check whether there is a naming server running at
    the host you give, nor whether the directory exists. Once a current hostname
    and current directory are set, you can abbreviate paths given to cd just as
    you can abbreviate the paths given to all other commands. For example, the
    following two sequences are equivalent:

        . dfs cd 127.0.0.1:/directory
        . dfs cd subdirectory

        . dfs cd 127.0.0.1:/directory/subdirectory

    The cd command, and all other commands, accepts the special path components
    . and .., referring to the current and parent directories, respectively.

    All commands except cd can be sourced (. dfs ls or source dfs ls) or run in
    a child process (./dfs ls).

./dfs ls
    Lists the current directory, if you have one set.

./dfs ls path ...
    Lists all the paths given on the command line. For each path that is a file,
    the filename is printed. For paths that refer to directories, the directory
    contents are printed.

./dfs pwd
    Prints the current hostname and directory, if you have these set.

./dfs mkdir path ...
    Creates the directories given on the command line.

./dfs touch file ...
    Creates the files given on the command line. Files that already exist are
    unaffected.

./dfs rm path ...
    Removes the files and/or directories given on the command line. Directories
    are deleted together with their contents.

./dfs get source_file destination_file
    Downloads a file from the distributed filesystem. The source file is a
    remote path, while the destination file is a local path. If the destination
    path refers to a directory, the command attempts to create a new file in the
    directory with the same name as the source file.

./dfs put source_file destination_file
    Uploads a file to the distributed filesystem. The source file is a local
    path, while the destination file is a remote path. If the destination path
    refers to a directory, the command attempts to create a new file in the
    directory with the same name as the source file.

./dfs parse path hostname
    Prints the effective hostname portion of the given path to standard output.
    This command is used internally by the cd command.

./dfs parse path path
    Printf the effective absolute path portion of the given path to standard
    output. This command is used internally by the cd command.

The dfs script can also be used to start naming and storage servers.

./dfs naming
    Starts a naming server running at the standard ports.

./dfs storage local_hostname naming_server directory
    Starts a storage server, with local_hostname being its externally-routable
    address, and naming_server the address of the naming server to contact. The
    storage server uses the given directory to store files. Files initially
    present in the directory are initially registered with the naming server.
```
