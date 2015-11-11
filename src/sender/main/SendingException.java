package sender.main;

import java.net.InetSocketAddress;


public class SendingException extends Exception {
    private final InetSocketAddress receiver;

    public SendingException(InetSocketAddress receiver) {
        this.receiver = receiver;
    }

    public InetSocketAddress getReceiver() {
        return receiver;
    }
}
