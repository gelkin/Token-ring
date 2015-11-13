import token.ring.NodeContext;

import java.io.IOException;
import java.net.NetworkInterface;

public class Main {

    public static void main(String[] args) throws IOException {
//        Collections.list(NetworkInterface.getNetworkInterfaces())
//                .forEach(System.out::println);

//        MessageSender sender = new MessageSender(NetworkInterface.getByName("wlan0"), 1247);
//        sender.unfreeze();
//        sender.broadcast(new LostTokenMsg());

        try (NodeContext nodeContext = new NodeContext(NetworkInterface.getByName("wlan0"), 1247)) {
            nodeContext.initiate();
            int stopComputation = System.in.read();
        }
    }

}
