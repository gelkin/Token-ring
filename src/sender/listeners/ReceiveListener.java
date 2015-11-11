package sender.listeners;

import sender.main.Message;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface ReceiveListener<ReplyType extends Message> {
    /**
     * Action performed when got an answer
     * @param source address to answer response with UDP (note: always UDP)
     * @param response answer itself
     */
    void onReceive(InetSocketAddress source, ReplyType response);
}
