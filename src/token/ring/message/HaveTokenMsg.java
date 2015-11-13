package token.ring.message;

import sender.main.RequestMessage;
import sender.message.VoidMessage;
import token.ring.Priority;

public class HaveTokenMsg extends RequestMessage<VoidMessage> {
    public Priority priority;

    public HaveTokenMsg(Priority priority) {
        this.priority = priority;
    }
}
