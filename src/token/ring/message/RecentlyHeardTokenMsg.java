package token.ring.message;

import org.apache.log4j.Logger;

public class RecentlyHeardTokenMsg extends AmCandidateResponseMsg {
    @Override
    public void logAboutThisMessage(Logger logger) {
        logger.info("Waiter says what recently heard from token");
    }
}
