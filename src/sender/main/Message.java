package sender.main;

import sender.message.MessageIdentifier;

import java.io.Serializable;

public abstract class Message implements Serializable {
    // Response messages copy identifier from corresponding request
    private MessageIdentifier identifier;

    public MessageIdentifier getIdentifier() {
        return identifier;
    }

    void setIdentifier(MessageIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return "Message <" + getClass().getSimpleName() + ">";
    }
}
