import sender.UniqueValue;
import sender.main.MessageSender;
import token.ring.message.LostTokenMsg;

import java.io.IOException;
import java.net.NetworkInterface;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        NetworkInterface network = NetworkInterface.getByName("wlan0");
        MessageSender sender = new MessageSender(UniqueValue.getLocal(network), network.getInetAddresses().nextElement(), 1247);

        sender.broadcast(new LostTokenMsg(), 5000, (source, response) -> System.out.println("Heard! " + response + " " + source));

        sender.close();

    }
}
