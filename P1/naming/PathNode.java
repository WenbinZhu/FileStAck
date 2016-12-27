package naming;

import common.Path;
import rmi.RMIException;
import storage.Command;
import storage.Storage;

import java.io.FileNotFoundException;
import java.util.HashMap;

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
    private ServerStubs serverStubs;
    private HashMap<String, PathNode> childNodes;

    public PathNode(Path nodePath, ServerStubs serverStubs)
    {
        this.nodePath = nodePath;
        this.serverStubs = serverStubs;
        this.childNodes = new HashMap<>();
    }

    public boolean isFile()
    {
        return serverStubs != null;
    }

    public Path getNodePath()
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
        if (serverStubs == null)
            throw new UnsupportedOperationException("Unable to add child to a leaf node");

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
}
