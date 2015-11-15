package token.ring.states;


import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import sender.message.ReminderFactory;
import token.ring.NodeContext;
import token.ring.NodeState;
import token.ring.message.*;

import java.util.Arrays;

public class WaiterState extends NodeState {
    private static final Logger logger = Logger.getLogger(WaiterState.class);

    public static final int WAITER_TIMEOUT = 4000;

    private final ReminderFactory waiterTimeoutRF = ReminderFactory.of(WaiterTimeoutExpireReminder::new, this::onTimeoutExpiration);

    private ReplyProtocol[] replyProtocols = new ReplyProtocol[]{
            ReplyProtocol.dumbOn(HaveTokenMsg.class, this::reactOnHaveTokenMsg),
            ReplyProtocol.on(RequestForNodeInfo.class, this::reactOnRequestForNodeInfo),
            ReplyProtocol.on(LostTokenMsg.class, __ -> new RecentlyHeardTokenMsg()),
            ReplyProtocol.on(AmCandidateMsg.class, __ -> new RecentlyHeardTokenMsg()),
            ReplyProtocol.on(PassTokenHandshakeMsg.class, __ -> new PassTokenHandshakeResponseMsg(ctx.getCurrentProgress(), ctx.sender.getTcpListenerAddress())),
            ReplyProtocol.on(AcceptToken.class, this::reactOnAcceptTokenMsg),
            waiterTimeoutRF
    };

    /**
     * Whether this should stay being LostToken and continue his lifecycle after timeout expires.
     * Transforms to LostTokenState otherwise as soon as timeout expires
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

    public void reactOnHaveTokenMsg(HaveTokenMsg haveTokenMsg) {
        logger.info("Heard from token");
        goingToStayAsIs = true;
    }

    public MyNodeInfoMsg reactOnRequestForNodeInfo(RequestForNodeInfo requestForNodeInfo) {
        logger.info("Heard RequestForNodeInfo from token");
        goingToStayAsIs = true;
        return new MyNodeInfoMsg(sender.getNodeInfo());
    }

    private void onTimeoutExpiration(WaiterTimeoutExpireReminder reminder) {
        if (goingToStayAsIs) {
            waitAndRefreshTimeout();
        } else {
            logger.info("Nothing interesting happened during timeout");
            ctx.switchToState(new LostTokenState(ctx));
        }
    }

    private AcceptTokenResponse reactOnAcceptTokenMsg(AcceptToken acceptToken) {
        if (acceptToken.computation.getCurrentPrecision() > ctx.piComputator.getCurrentPrecision()) {
            ctx.piComputator = acceptToken.computation;
        }

        logger.info(String.format("Got pi number. Current progress: %d, last %d digits: %s",
                ctx.getCurrentProgress(), ctx.PI_PRECISION_STEP, ctx.piComputator.getLastDigits()));

        ctx.netmap = acceptToken.netmap;

        ctx.switchToState(new TokenHolderState(ctx));

        return new AcceptTokenResponse();
    }

}
