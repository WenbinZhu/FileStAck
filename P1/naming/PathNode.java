package naming;

import common.Path;
import rmi.RMIException;

import java.io.FileNotFoundException;
import java.util.HashMap;

/** File node in naming server's directory tree
 */
public class PathNode
{
    private Path nodePath;
    private boolean isFile;
    private HashMap<String, PathNode> childNodes;

    public PathNode(boolean isFile, Path nodePath)
    {
        this.isFile = isFile;
        this.nodePath = nodePath;
        this.childNodes = new HashMap<>();
    }

    public boolean isFile()
    {
        return isFile;
    }

    public PathNode getChild(String component)
    {
        return childNodes.get(component);
    }

    public HashMap<String, PathNode> getChildren()
    {
        return childNodes;
    }

    public void addChild(String component, PathNode child) throws RMIException
    {
        if (childNodes.containsKey(component))
            throw new RMIException("Unable to add an existing node again");

        childNodes.put(component, child);
    }

    public void deleteChild(String component) throws RMIException
    {
        if (!childNodes.containsKey(component))
            throw new RMIException("Unable to delete a non-existing node");

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
