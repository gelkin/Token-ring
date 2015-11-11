package token.ring;

import org.apache.log4j.Logger;
import sender.main.MessageSender;
import sender.listeners.ReplyProtocol;
import sender.message.VoidMessage;
import token.ring.message.AmCandidateMsg;
import token.ring.message.HaveTokenMsg;
import token.ring.message.LostTokenMsg;
import token.ring.message.LostTokenTimeoutExpireReminder;

import java.util.Arrays;

public class LostTokenState implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(LostTokenState.class);

    public static final int LOST_TOKEN_TIMEOUT = 5000;

    private ReplyProtocol[] replyProtocols = new ReplyProtocol[]{
            new HaveTokenRp(),
            new AmCandidateRp(),
            new TimeoutExpireReminderRp()
    };

    private final Context ctx;
    private final MessageSender sender;

    /**
     * Whether this should stay being LostToken and continue his lifecycle after timeout expires.
     * Transforms to CandidateState otherwise
     */
    private boolean goingToStayAsIs = false;

    public LostTokenState(Context ctx) {
        this.ctx = ctx;
        this.sender = ctx.sender;
    }

    public void start() {
        Arrays.stream(replyProtocols).forEach(sender::registerReplyProtocol);
        broadcastAndRefreshTimeout();
    }

    private void broadcastLostToken() {
        sender.broadcast(new LostTokenMsg(), LOST_TOKEN_TIMEOUT,
                (address, recentlyHeardTokenMsg) -> {
                    if (goToStayAsIs()) {
                        logger.info("[LOST_TOKEN] Received RecentlyHeardTokenMsg, going to repeat lifecycle");
                    }
                }
        );
    }

    private void broadcastAndRefreshTimeout() {
        logger.info("Refreshing timeout, sending LostTokenMsg");

        // broadcasts LostTokenMsg

        // if in next LOST_TOKEN_TIMEOUT got RecentlyHeardTokenMsg or AmCandidateMsg with higher priority,
        // sets goingToStayAsIs to true and repeats from beginning when timeout expires.
        // If got nothing during timeout, switches to CandidateState
        goingToStayAsIs = false;
        broadcastLostToken();
        sender.remind(new LostTokenTimeoutExpireReminder(), LOST_TOKEN_TIMEOUT);
    }

    /**
     * Sets goingToStayAsIs to true
     * @return whether this method invocation changed value of goingToStayAsIs
     */
    private boolean goToStayAsIs() {
        // All guys at military faculty heard as Ruslan said about this code:
        // "This is useless in practice".
        // It isn't at all
        try {
            return !goingToStayAsIs;
        } finally {
            goingToStayAsIs = true;
        }
    }

    @Override
    public void close() {
    }

    private class HaveTokenRp implements ReplyProtocol<HaveTokenMsg, VoidMessage> {
        @Override
        public VoidMessage makeResponse(HaveTokenMsg haveTokenMsg) {
            logger.info("Heard from token");
            ctx.switchToState("waiter");
            return null;
        }
    }

    private class AmCandidateRp implements ReplyProtocol<AmCandidateMsg, VoidMessage> {
        @Override
        public VoidMessage makeResponse(AmCandidateMsg amCandidateMsg) {
            int isHisGreater = amCandidateMsg.priority.compareTo(ctx.getPriority());
            if (isHisGreater == -1) {
                // set goingToStayAsIs to true and notify if it is a first such message
                if (goToStayAsIs()) {
                    infoAboutMessage(amCandidateMsg, "[LOST_TOKEN] Received from candidate with higher priority %s (our priority is %s), going to repeat lifecycle");
                }
            } else if (isHisGreater == 1) {
                infoAboutMessage(amCandidateMsg, "[LOST_TOKEN] Received from candidate with lower priority %s (our priority is %s)");
                ctx.switchToState("candidate");
            } else {
                logger.error("WTF? Got I_AM_CANDIDATE_MSG with same priority as mine!");
            }
            return null;
        }

        private void infoAboutMessage(AmCandidateMsg amCandidateMsg, String text) {
            logger.info(String.format(text, amCandidateMsg.priority, ctx.getPriority()));
        }
    }

    private class TimeoutExpireReminderRp implements ReplyProtocol<LostTokenTimeoutExpireReminder, VoidMessage> {
        @Override
        public VoidMessage makeResponse(LostTokenTimeoutExpireReminder reminder) {
            if (goingToStayAsIs) {
                broadcastAndRefreshTimeout();
            } else {
                logger.info("Nothing interesting happened during timeout");
                ctx.switchToState("candidate");
            }
            return null;
        }
    }

}
