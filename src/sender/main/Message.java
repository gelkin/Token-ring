package sender.main;

import sender.message.MessageIdentifier;

import java.io.Serializable;

public class Message implements Serializable {
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
