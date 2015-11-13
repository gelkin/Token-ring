import token.ring.NodeContext;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collections;

public class Main {
    public static void main(String[] args) throws IOException {
        try (NodeContext nodeContext = new NodeContext(NetworkInterface.getByName("wlan0"), 1247)) {
            nodeContext.initiate();
            int stopComputation = System.in.read();
        }
    }
}
