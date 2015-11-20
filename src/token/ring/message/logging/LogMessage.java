package token.ring.message.logging;

import sender.main.RequestMessage;
import sender.message.VoidMessage;

public class LogMessage extends RequestMessage<VoidMessage> {
    @Override
    protected boolean logOnSend() {
        return false;
    }
}
