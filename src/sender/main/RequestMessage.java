package sender.main;

import java.net.InetSocketAddress;

public class RequestMessage<ReplyType extends ResponseMessage> extends Message {
    private InetSocketAddress responseListenerAddress;

    InetSocketAddress getResponseListenerAddress() {
        return responseListenerAddress;
    }

    void setResponseListenerAddress(InetSocketAddress responseListenerAddress) {
        this.responseListenerAddress = responseListenerAddress;
    }

}
