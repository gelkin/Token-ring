package token.ring.message;

import sender.main.ResponseMessage;

import java.net.InetSocketAddress;

public class PassTokenHandshakeResponseMsg extends ResponseMessage {
    public final int progress;
    public final InetSocketAddress tcpAddress;

    public PassTokenHandshakeResponseMsg(int progress, InetSocketAddress tcpAddress) {
        this.progress = progress;
        this.tcpAddress = tcpAddress;
    }

}
