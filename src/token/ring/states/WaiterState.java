package token.ring.states;


import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import sender.message.ReminderFactory;
import sender.message.VoidMessage;
import token.ring.NodeContext;
import token.ring.NodeState;
import token.ring.message.*;

import java.util.Arrays;

public class WaiterState extends NodeState {
    private static final Logger logger = Logger.getLogger(WaiterState.class);

    public static final int WAITER_TIMEOUT = 4000;

    private final WaiterTimeoutRF waiterTimeoutRF = new WaiterTimeoutRF();

    private ReplyProtocol[] replyProtocols = new ReplyProtocol[]{
            new HaveTokenRp(),
            new RequestForNodeInfoRp(),
            new LostTokenRp(),
            new AmCandidateRp(),
            waiterTimeoutRF
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
        sender.remind(waiterTimeoutRF.newReminder(), WAITER_TIMEOUT);
    }

    private class HaveTokenRp implements ReplyProtocol<HaveTokenMsg, VoidMessage> {
        @Override
        public VoidMessage makeResponse(HaveTokenMsg haveTokenMsg) {
            logger.info("Heard from token");
            goingToStayAsIs = true;
            return null;
        }

        @Override
        public Class<? extends HaveTokenMsg> requestType() {
            return HaveTokenMsg.class;
        }
    }

    private class RequestForNodeInfoRp implements ReplyProtocol<RequestForNodeInfo, MyNodeInfoMsg> {
        @Override
        public MyNodeInfoMsg makeResponse(RequestForNodeInfo msg) {
            logger.info("Heard RequestForNodeInfo from token");
            goingToStayAsIs = true;
            return new MyNodeInfoMsg(sender.getNodeInfo());
        }

        @Override
        public Class<? extends RequestForNodeInfo> requestType() {
            return RequestForNodeInfo.class;
        }
    }

    private class LostTokenRp implements ReplyProtocol<LostTokenMsg, RecentlyHeardTokenMsg> {
        @Override
        public RecentlyHeardTokenMsg makeResponse(LostTokenMsg lostTokenMsg) {
            return new RecentlyHeardTokenMsg();
        }

        @Override
        public Class<? extends LostTokenMsg> requestType() {
            return LostTokenMsg.class;
        }
    }

    private class AmCandidateRp implements ReplyProtocol<AmCandidateMsg, AmCandidateResponseMsg> {
        @Override
        public RecentlyHeardTokenMsg makeResponse(AmCandidateMsg amCandidateMsg) {
            return new RecentlyHeardTokenMsg();
        }

        @Override
        public Class<? extends AmCandidateMsg> requestType() {
            return AmCandidateMsg.class;
        }
    }

    private class WaiterTimeoutRF extends ReminderFactory<WaiterTimeoutExpireReminder> {
        public WaiterTimeoutRF() {
            super(WaiterTimeoutExpireReminder::new);
        }

        @Override
        protected void onRemind(WaiterTimeoutExpireReminder reminder) {
            if (goingToStayAsIs) {
                waitAndRefreshTimeout();
            } else {
                logger.info("Nothing interesting happened during timeout");
                ctx.switchToState(new LostTokenState(ctx));
            }
        }

        @Override
        public Class<? extends WaiterTimeoutExpireReminder> requestType() {
            return WaiterTimeoutExpireReminder.class;
        }
    }
}
