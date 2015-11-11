import sender.UniqueValue;
import sender.main.MessageSender;

import java.io.IOException;
import java.net.NetworkInterface;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        MessageSender sender = new MessageSender(UniqueValue.getLocal(NetworkInterface.getByName("wlan0")), 1247);

        sender.close();

    }
}
