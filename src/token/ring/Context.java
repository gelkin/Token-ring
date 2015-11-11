package token.ring;

import sender.main.MessageSender;

public class Context {
    MessageSender sender;

    void switchToState(String state){ // TODO: it will accept state of course
        // freezes sender
        // closes current state
        // logs about state switching
        // stores and starts specified state
        // unfreezes sender
    }

    public Priority getPriority() {
        return null;
    }
}
