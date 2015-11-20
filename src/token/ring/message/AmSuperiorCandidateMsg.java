package token.ring.message;

import org.apache.log4j.Logger;

public class AmSuperiorCandidateMsg extends AmCandidateResponseMsg {
    @Override
    public void logAboutThisMessage(Logger logger) {
        logger.info("Detected a superior candidate");
    }
}
