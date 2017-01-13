package naming;

import common.Path;
import storage.Command;
import storage.Storage;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** Storage and Command stub pair
 */
class ServerStubs
{
    public Storage storageStub;
    public Command commandStub;

    public ServerStubs(Storage storageStub, Command commandStub)
    {
        this.storageStub = storageStub;
        this.commandStub = commandStub;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServerStubs that = (ServerStubs) o;

        return storageStub.equals(that.storageStub) && commandStub.equals(that.commandStub);
    }

    @Override
    public int hashCode() {
        int result = storageStub.hashCode();
        result = 31 * result + commandStub.hashCode();

        return result;
    }
}

/** File node in naming server's directory tree
 */
public class PathNode
{
    private Path nodePath;
    private int accessTime;
    private ServerStubs serverStubs;    // Storage server of the original copy
    private HashSet<ServerStubs> replicaStubs;
    private HashMap<String, PathNode> childNodes;

    public PathNode(Path nodePath, ServerStubs serverStubs)
    {
        this.nodePath = nodePath;
        this.accessTime = 0;
        this.serverStubs = serverStubs;
        this.replicaStubs = new HashSet<>();
        this.childNodes = new HashMap<>();
    }

    public boolean isFile()
    {
        return serverStubs != null;
    }

    public Path getPath()
    {
        return nodePath;
    }

    public ServerStubs getStubs()
    {
        return serverStubs;
    }

    public void setStubs(ServerStubs stubs)
    {
        serverStubs = stubs;
    }

    public HashMap<String, PathNode> getChildren()
    {
        return childNodes;
    }

    public void addChild(String component, PathNode child) throws UnsupportedOperationException
    {
        // if (isFile())
        //     throw new UnsupportedOperationException("Unable to add child to a leaf node");

        if (childNodes.containsKey(component))
            throw new UnsupportedOperationException("Unable to add an existing node again");

        childNodes.put(component, child);
    }

    public void deleteChild(String component) throws UnsupportedOperationException
    {
        if (!childNodes.containsKey(component))
            throw new UnsupportedOperationException("Unable to delete a non-existing node");

        childNodes.remove(component);
    }

    /** Get PathNode from the directory tree given a path
     */
    public PathNode getNodeByPath(Path path) throws FileNotFoundException
    {
        PathNode curNode = this;

        for (String component : path) {
            if (!curNode.childNodes.containsKey(component))
                throw new FileNotFoundException("Unable to get node from path");

            curNode = curNode.childNodes.get(component);
        }

        return curNode;
    }

    /** Get all the descendant nodes which refers to a file
     */
    public ArrayList<PathNode> getDescendants() {
        ArrayList<PathNode> descendants = new ArrayList<>();

        for (PathNode node : childNodes.values()) {
            if (node.isFile())
                descendants.add(node);
            else
                descendants.addAll(node.getDescendants());
        }

        return descendants;
    }

    /** Increase the node's access time

        <p>
        Return true if the access time is beyond the pre-set multiple
        and then reset the access time to 0.
     */
    public boolean incAccessTime(int multiple) {
        if (++accessTime > multiple) {
            accessTime = 0;
            return true;
        }

        return false;
    }

    public void resetAccessTime() {
        accessTime = 0;
    }

    public HashSet<ServerStubs> getReplicaStubs() {
        return replicaStubs;
    }

    public void addReplicaStub(ServerStubs serverStubs) {
        // Naming server will ensure the nodes calling
        // this method refers to a file, not a directory
        replicaStubs.add(serverStubs);
    }

    public int getReplicaSize() {
        return replicaStubs.size();
    }

    public void removeReplicaStub(ServerStubs serverStubs) {
        replicaStubs.remove(serverStubs);
    }
}
