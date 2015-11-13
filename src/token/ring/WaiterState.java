package token.ring;


import org.apache.log4j.Logger;
import sender.MessageSender;
import sender.ReplyProtocol;
import token.ring.message.*;

import java.util.Arrays;

public class WaiterState implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(WaiterState.class);

    public static final int WAITER_TIMEOUT = 4000;

    private ReplyProtocol[] replyProtocols = new ReplyProtocol[]{
            new HaveTokenRp(),
            new LostTokenRp(),
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

    public WaiterState(Context ctx) {
        this.ctx = ctx;
        this.sender = ctx.sender;
    }

    public void start() {
        Arrays.stream(replyProtocols).forEach(sender::registerReplyProtocol);
        waitAndRefreshTimeout();
    }

    private void waitAndRefreshTimeout() {
        logger.info("Waiting for messages");

        // if in next WAITER_TIMEOUT got LostTokenMsg or AmCandidateMsg, replies with
        // RecentlyHeardTokenMsg, if got HaveTokenMsg, sets goingToStayAsIs to true
        // and repeats from beginning when timeout expires.
        // If got nothing during timeout, switches to LostTokenState
        goingToStayAsIs = false;
        sender.remind(new WaiterTimeoutExpireReminder(), WAITER_TIMEOUT);
    }

    @Override
    public void close() {
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

    private class HaveTokenRp implements ReplyProtocol<HaveTokenMsg, VoidOrRecentlyHeard> {
        @Override
        public RecentlyHeardTokenMsg makeResponse(HaveTokenMsg haveTokenMsg) {
            logger.info("Heard from token");
            goingToStayAsIs = true;
            return null;
        }
    }

    private class LostTokenRp implements ReplyProtocol<LostTokenMsg, RecentlyHeardTokenMsg> {
        @Override
        public RecentlyHeardTokenMsg makeResponse(LostTokenMsg lostTokenMsg) {
            return new RecentlyHeardTokenMsg();
        }
    }

    private class AmCandidateRp implements ReplyProtocol<AmCandidateMsg, RecentlyHeardTokenMsg> {
        @Override
        public RecentlyHeardTokenMsg makeResponse(AmCandidateMsg amCandidateMsg) {
            return new RecentlyHeardTokenMsg();
        }
    }

    private class TimeoutExpireReminderRp implements ReplyProtocol<LostTokenTimeoutExpireReminder, sender.message.RecentlyHeardTokenMsg> {
        @Override
        public sender.message.RecentlyHeardTokenMsg makeResponse(LostTokenTimeoutExpireReminder reminder) {
            if (goingToStayAsIs) {
                waitAndRefreshTimeout();
            } else {
                logger.info("Nothing interesting happened during timeout");
                ctx.switchToState(LostTokenState);
            }
            return null;
        }
    }
}
