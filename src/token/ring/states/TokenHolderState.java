package token.ring.states;

import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import sender.main.MessageSender;
import sender.message.VoidMessage;
import token.ring.NodeContext;
import token.ring.NodeInfo;
import token.ring.NodeState;
import token.ring.Priority;
import token.ring.message.*;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;

public class TokenHolderState extends NodeState {
    private static final Logger logger = Logger.getLogger(TokenHolderState.class);

    public static final int TIC = 5000;
    public static final int IDLE_TIME = 2000;

    /**
     * At any time equals to !(IDLE_TIME time went since became Token holder) + !(computed pi) + !(currently waiting for NodeInfo)
     */
    private int stagesRemained = 2;

    private NodeInfo acceptingTokenNode;

    private ReplyProtocol[] replyProtocols = new ReplyProtocol[]{
            new BroadcastHaveTokenReminderRp(),
            new ListenToOtherTokenHoldersRp(),
            new IdlingTimeoutExpirationRp()
    };

    public TokenHolderState(NodeContext ctx) {
        super(ctx);
    }

    @Override
    public void start() {
        Arrays.stream(replyProtocols).forEach(sender::registerReplyProtocol);

        sender.remind(new TimeoutExpireReminder(), 0);
        sender.remind(new IdlingTimeoutExpiredReminder(), IDLE_TIME);
        // TODO: run pi computation

        if (decideWhetherToUpdateNetMap()) {
            stagesRemained++;
            sender.broadcast(new RequestForNodeInfo(), 5000,
                    (source, response) -> ctx.netmap.add(response.nodeInfo),
                    this::markStageCompleted);
        }

    }

    private boolean decideWhetherToUpdateNetMap() {
        // true with probability 1 / n, where n is network map size
        return new Random().nextInt(ctx.netmap.size()) == 0;
    }

    private void markStageCompleted() {
        stagesRemained--;
        if (stagesRemained == 0) {
            logger.info("All the business done, going to pass token");
            passToken();
        }
    }

    private void passToken() {
        if (ctx.netmap.size() == 1) {
            logger.info("No more nodes are known");
            // TODO: what to do next?
            // repeat (temporal solution)
            ctx.switchToState(new TokenHolderState(ctx));
        } else {
            acceptingTokenNode = ctx.netmap.getNextFrom(sender.getNodeInfo());
            sender.send(new InetSocketAddress(acceptingTokenNode.address, 0), new PassTokenHandshakeMsg(), MessageSender.DispatchType.UDP, 5000,
                    (source, response) -> passTokenStage2(response),
                    this::passTokenFail
            );
        }

    }

    private void passTokenStage2(PassTokenHandshakeResponseMsg handshakeResponse) {
        logger.info("Handshake success, passing token");
        sender.send(handshakeResponse.tcpAddress, new AcceptToken(), MessageSender.DispatchType.TCP, 5000,
                (source, response) -> {
                    logger.info("Token successfully passed");
                    ctx.switchToState(new WaiterState(ctx));
                },
                this::passTokenFail
        );
    }

    private void passTokenFail() {
        logger.info(String.format("Token passing to node %s failed. Trying again", acceptingTokenNode));
        ctx.netmap.remove(acceptingTokenNode);
        acceptingTokenNode = null;
        passToken();
    }

    @Override
    public void close() {
    }

    private class BroadcastHaveTokenReminderRp implements ReplyProtocol<TimeoutExpireReminder, VoidMessage> {
        @Override
        public VoidMessage makeResponse(TimeoutExpireReminder timeoutExpireReminder) {
            sender.broadcast(new HaveTokenMsg(ctx.getCurrentPriority()), TIC);
            sender.remind(new TimeoutExpireReminder(), TIC);
            return null;
        }
    }

    private class ListenToOtherTokenHoldersRp implements ReplyProtocol<HaveTokenMsg, VoidMessage> {
        @Override
        public VoidMessage makeResponse(HaveTokenMsg haveTokenMsg) {
            Priority outPriority = ctx.getCurrentPriority();
            if (outPriority.compareTo(haveTokenMsg.priority) < 0) {
                logger.info(String.format("Detected token holder with higher priority %s (our priority is %s)", haveTokenMsg.priority, outPriority));
                ctx.switchToState(new WaiterState(ctx));
            }
            return null;
        }
    }

    private class IdlingTimeoutExpirationRp implements ReplyProtocol<IdlingTimeoutExpiredReminder, VoidMessage> {
        @Override
        public VoidMessage makeResponse(IdlingTimeoutExpiredReminder haveTokenMsg) {
            logger.info("Idle period passed");
            stagesRemained--;
            return null;
        }
    }


}
