package token.ring.states;

import misc.Colorer;
import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import sender.message.ReminderFactory;
import token.ring.NodeContext;
import token.ring.NodeState;
import token.ring.ScaredOfTokenMsgs;
import token.ring.message.*;

import java.util.stream.Stream;

public class LostTokenState extends NodeState {
    private static final Logger logger = Logger.getLogger(LostTokenState.class);

    public static final int LOST_TOKEN_TIMEOUT = 5000;

    private final ReminderFactory timeoutExpireRF = ReminderFactory.of(LostTokenTimeoutExpireReminder::new, this::onTimeoutExpire);

    /**
     * Whether this should stay being LostToken and continue his lifecycle after timeout expires.
     * Transforms to CandidateState otherwise as soon as timeout expires
     */
    private boolean goingToStayAsIs = false;

    public LostTokenState(NodeContext ctx) {
        super(ctx);
    }

    public void start() {
        Stream.concat(
                new ScaredOfTokenMsgs(ctx, logger).getProtocols(),
                Stream.of(
                        new AmCandidateRp(),
                        timeoutExpireRF
                )
        ).forEach(sender::registerReplyProtocol);

        broadcastAndRefreshTimeout();
    }

    private void broadcastLostToken() {
        sender.broadcast(new LostTokenMsg(), LOST_TOKEN_TIMEOUT,
                (address, recentlyHeardTokenMsg) -> {
                    // set goingToStayAsIs to true and notify if it is a first such message
                    if (goToStayAsIs()) {
                        logger.info("Received RecentlyHeardTokenMsg, going to repeat lifecycle");
                    }
                }
        );
    }

    private void broadcastAndRefreshTimeout() {
        logger.info("Refreshing timeout, broadcasting LostTokenMsg");

        // broadcasts LostTokenMsg

        // if in next LOST_TOKEN_TIMEOUT got RecentlyHeardTokenMsg or AmCandidateMsg with higher priority,
        // sets goingToStayAsIs to true and repeats from beginning when timeout expires.
        // If got nothing during timeout, switches to CandidateState
        goingToStayAsIs = false;
        broadcastLostToken();
        sender.remind(timeoutExpireRF.newReminder(), LOST_TOKEN_TIMEOUT);
    }

    /**
     * Sets goingToStayAsIs to true
     *
     * @return whether this method invocation changed value of goingToStayAsIs
     */
    private boolean goToStayAsIs() {
        // All guys at military faculty heard as Ruslan said about such usage of try-finally:
        // "This is useless in practice".
        // It isn't at all
        try {
            return !goingToStayAsIs;
        } finally {
            goingToStayAsIs = true;
        }
    }

    private void onTimeoutExpire(LostTokenTimeoutExpireReminder reminder) {
        if (goingToStayAsIs) {
            broadcastAndRefreshTimeout();
        } else {
            logger.info("Nothing interesting happened during timeout");
            ctx.switchToState(new CandidateState(ctx));
        }
    }

    private class AmCandidateRp implements ReplyProtocol<AmCandidateMsg, AmCandidateResponseMsg> {
        @Override
        public RecentlyHeardTokenMsg makeResponse(AmCandidateMsg amCandidateMsg) {
            int isHisGreater = amCandidateMsg.priority.compareTo(ctx.getCurrentPriority());
            if (isHisGreater < 0) {
                // set goingToStayAsIs to true and notify if it is a first such message
                if (goToStayAsIs()) {
                    logAboutMessage(amCandidateMsg, Colorer.format("Received from candidate with %1`higher%` priority %s (our priority is %s), going to repeat lifecycle"));
                }
            } else if (isHisGreater > 0) {
                logAboutMessage(amCandidateMsg, Colorer.format("Received from candidate with %2`lower%` priority %s (our priority is %s)"));
                ctx.switchToState(new CandidateState(ctx));
            } else {
                logger.error("WTF? Got AmCandidateMsg with same priority as mine!");
            }
            return null;
        }

        private void logAboutMessage(AmCandidateMsg amCandidateMsg, String text) {
            logger.info(String.format(text, amCandidateMsg.priority, ctx.getCurrentPriority()));
        }

        @Override
        public Class<? extends AmCandidateMsg> requestType() {
            return AmCandidateMsg.class;
        }
    }

}
