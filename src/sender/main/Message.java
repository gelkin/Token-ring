package sender.main;

import sender.message.MessageIdentifier;

import java.io.Serializable;
import java.net.InetSocketAddress;

public abstract class Message implements Serializable {
    // Response messages copy identifier from corresponding request
    private MessageIdentifier identifier;
    private InetSocketAddress responseListenerAddress;

    public MessageIdentifier getIdentifier() {
        return identifier;
    }

    final void setIdentifier(MessageIdentifier identifier) {
        this.identifier = identifier;
    }

    final InetSocketAddress getResponseListenerAddress() {
        return responseListenerAddress;
    }

    final void setResponseListenerAddress(InetSocketAddress responseListenerAddress) {
        this.responseListenerAddress = responseListenerAddress;
    }

    // tuning options
    protected boolean logOnSend() {
        return true;
    }

    protected boolean logOnReceive() {
        return true;
    }

    @Override
    public String toString() {
        return "Message <" + getClass().getSimpleName() + ">";
    }
}
