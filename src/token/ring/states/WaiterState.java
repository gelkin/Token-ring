package token.ring.states;


import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import sender.message.VoidMessage;
import token.ring.NodeContext;
import token.ring.NodeInfo;
import token.ring.NodeState;
import token.ring.message.*;

import java.util.Arrays;

public class WaiterState extends NodeState {
    private static final Logger logger = Logger.getLogger(WaiterState.class);

    public static final int WAITER_TIMEOUT = 4000;

    private ReplyProtocol[] replyProtocols = new ReplyProtocol[]{
            new HaveTokenRp(),
            new RequestForNodeInfoRp(),
            new LostTokenRp(),
            new AmCandidateRp(),
            new TimeoutExpireReminderRp()
    };

    /**
     * Whether this should stay being LostToken and continue his lifecycle after timeout expires.
     * Transforms to CandidateState otherwise
     */
    private boolean goingToStayAsIs = false;

    public WaiterState(NodeContext ctx) {
        super(ctx);
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

    private class HaveTokenRp implements ReplyProtocol<HaveTokenMsg, VoidMessage> {
        @Override
        public VoidMessage makeResponse(HaveTokenMsg haveTokenMsg) {
            logger.info("Heard from token");
            goingToStayAsIs = true;
            return null;
        }
    }

    private class RequestForNodeInfoRp implements ReplyProtocol<RequestForNodeInfo, MyNodeInfoMsg> {
        @Override
        public MyNodeInfoMsg makeResponse(RequestForNodeInfo msg) {
            logger.info("Heard RequestForNodeInfo from token");
            goingToStayAsIs = true;
            return new MyNodeInfoMsg(sender.getNodeInfo());
        }
    }

    private class LostTokenRp implements ReplyProtocol<LostTokenMsg, RecentlyHeardTokenMsg> {
        @Override
        public RecentlyHeardTokenMsg makeResponse(LostTokenMsg lostTokenMsg) {
            return new RecentlyHeardTokenMsg();
        }
    }

    private class AmCandidateRp implements ReplyProtocol<AmCandidateMsg, AmCandidateResponseMsg> {
        @Override
        public RecentlyHeardTokenMsg makeResponse(AmCandidateMsg amCandidateMsg) {
            return new RecentlyHeardTokenMsg();
        }
    }

    private class TimeoutExpireReminderRp implements ReplyProtocol<WaiterTimeoutExpireReminder, VoidMessage> {
        @Override
        public VoidMessage makeResponse(WaiterTimeoutExpireReminder reminder) {
            if (goingToStayAsIs) {
                waitAndRefreshTimeout();
            } else {
                logger.info("Nothing interesting happened during timeout");
                ctx.switchToState(new LostTokenState(ctx));
            }
            return null;
        }
    }
}
