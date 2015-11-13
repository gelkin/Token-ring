import sender.listeners.ReplyProtocol;
import sender.main.MessageSender;
import sender.message.ReminderMessage;
import sender.message.VoidMessage;
import token.ring.message.LostTokenMsg;
import token.ring.message.RecentlyHeardTokenMsg;

import java.io.IOException;
import java.net.NetworkInterface;

public class TestSender {
    public static void main(String[] args) throws IOException, InterruptedException {
        MessageSender sender = new MessageSender(NetworkInterface.getByName("wlan0"), 1247);

        ReplyProtocol<LostTokenMsg, RecentlyHeardTokenMsg> protocol = lostTokenMsg -> {
            System.out.println("Heard!!");
            return new RecentlyHeardTokenMsg();
        };

        ReplyProtocol<ReminderMessage, VoidMessage> reminderProtocol = new ReplyProtocol<ReminderMessage, VoidMessage>() {
            @Override
            public VoidMessage makeResponse(ReminderMessage reminderMessage) {
                sender.freeze();
                return null;
            }
        };

        while (true) {
            sender.registerReplyProtocol(protocol);
            sender.registerReplyProtocol(reminderProtocol);
            sender.unfreeze();

            Thread.sleep(100);
            sender.remind(new ReminderMessage(), 0);
            Thread.sleep(3000);
        }
//        sender.close();

    }
}
