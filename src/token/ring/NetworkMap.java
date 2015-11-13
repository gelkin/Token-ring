package token.ring;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Precautions:
 *
 */
public class NetworkMap implements Serializable {
    private final List<NodeInfo> nodes = new LinkedList<>();

    public NetworkMap(NodeInfo myNode) {
        nodes.add(myNode);
    }

    public void add(NodeInfo nodeInfo) {
        // prefer updated info
        nodes.remove(nodeInfo);
        nodes.add(nodeInfo);
    }

    public NodeInfo getNextFrom(NodeInfo myNode) {
        add(myNode);

        boolean returnHere = false;
        for (NodeInfo node : nodes) {
            if (returnHere)
                return node;

            if (node.equals(myNode))
                returnHere = true;
        }
        if (returnHere) {
            return nodes.get(0);
        } else {
            throw new Error("Unexpected behaviour, no myNode found in map");
        }
    }

    public int size() {
        return nodes.size();
    }

    public void remove(NodeInfo nodeInfo) {
        nodes.remove(nodeInfo);
    }
}
