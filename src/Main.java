import sender.UniqueValue;
import sender.main.Message;
import sender.main.MessageSender;
import token.ring.message.LostTokenMsg;
import token.ring.message.RecentlyHeardTokenMsg;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        NetworkInterface network = NetworkInterface.getByName("wlan0");
        MessageSender sender = new MessageSender(UniqueValue.getLocal(network), network.getInetAddresses().nextElement(), 1247);
        sender.unfreeze();

        sender.broadcast(new LostTokenMsg(), 5000, (source, response) -> System.out.println("Heard! " + response + " " + source));

//        Stream<RecentlyHeardTokenMsg> responses = sender.broadcastAndWait(new LostTokenMsg(), 5000);
//        responses.findAny().ifPresent(System.out::println);

//        sender.close();

    }
}
