package token.ring.message;

import sender.main.RequestMessage;
import token.ring.Priority;

public class AmCandidateMsg extends RequestMessage<AmCandidateResponseMsg> {
    public final Priority priority;

    public AmCandidateMsg(Priority priority) {
        this.priority = priority;
    }

}
