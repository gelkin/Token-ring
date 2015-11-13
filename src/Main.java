import sender.listeners.ReplyProtocol;
import sender.main.MessageSender;
import token.ring.message.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

public class Main {

    public static void main(String[] args) throws IOException {
//        Collections.list(NetworkInterface.getNetworkInterfaces())
//                .forEach(System.out::println);

        MessageSender sender = new MessageSender(NetworkInterface.getByName("wlan0"), 1247);
        sender.registerReplyProtocol(ReplyProtocol.of(AmCandidateMsg.class, amCandidateMsg -> new AmSuperiorCandidateMsg()));
        sender.registerReplyProtocol(ReplyProtocol.of(LostTokenMsg.class, lostTokenMsg -> new RecentlyHeardTokenMsg()));
        sender.send(new InetSocketAddress(InetAddress.getByName("192.168.0.103"), 1247), new AmCandidateMsg(null), MessageSender.DispatchType.UDP, 1000, (a, b) -> System.out.println("lol"));

        sender.unfreeze();
//        try (NodeContext nodeContext = new NodeContext(NetworkInterface.getByName("wlan0"), 1247)) {
//            nodeContext.initiate();
//            int stopComputation = System.in.read();
//        }
    }

}
