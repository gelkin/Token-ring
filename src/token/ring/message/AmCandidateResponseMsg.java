package token.ring.message;

import org.apache.log4j.Logger;
import sender.main.ResponseMessage;

public abstract class AmCandidateResponseMsg extends ResponseMessage {
    /*
    Processing of derived classes differs.
    You can replace this method with smarter one, or replace code occurrences with switch whenever you want.
     */
    public abstract void logAboutThisMessage(Logger logger);
}
