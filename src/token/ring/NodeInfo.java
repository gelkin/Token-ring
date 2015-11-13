package token.ring;

import java.io.Serializable;
import java.net.InetAddress;

public class NodeInfo implements Serializable {
    public final InetAddress address;

    // Other fields are useless
    private final int tcpPort;
    private final UniqueValue unique;

    public NodeInfo(InetAddress address, int tcpPort, UniqueValue unique) {
        this.address = address;
        this.tcpPort = tcpPort;
        this.unique = unique;
    }

//    public InetSocketAddress getTcpListenerAddress() {
//        return new InetSocketAddress(address, tcpPort);
//    }

    @Override
    public String toString() {
        return String.format("NodeInfo [%s] {%s, %s}", unique, address, tcpPort);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeInfo nodeInfo = (NodeInfo) o;

        return unique.equals(nodeInfo.unique);

    }

    @Override
    public int hashCode() {
        return unique.hashCode();
    }
}
