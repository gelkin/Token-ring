package token.ring.states;

import misc.Colorer;
import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import sender.main.DispatchType;
import sender.message.ReminderFactory;
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

    private final ReminderFactory idlingTimeoutExpirationRF = ReminderFactory.of(IdlingTimeoutExpiredReminder::new, this::onIdleTimeoutExpiration);
    private final ReminderFactory broadcastHaveTokenRF = ReminderFactory.of(TokenHolderTimeoutExpireReminder::new, this::onTokenHolderTimeoutExpiration);
    private ReplyProtocol[] replyProtocols = new ReplyProtocol[]{
            broadcastHaveTokenRF,
            ReplyProtocol.dumbOn(HaveTokenMsg.class, this::onHearFromOtherToken),
            idlingTimeoutExpirationRF
    };

    public TokenHolderState(NodeContext ctx) {
        super(ctx);
    }

    @Override
    public void start() {
        Arrays.stream(replyProtocols).forEach(sender::registerReplyProtocol);

        sender.remind(broadcastHaveTokenRF.newReminder(), 0);
        sender.remind(idlingTimeoutExpirationRF.newReminder(), IDLE_TIME);
        ctx.executor.submit(() -> {
            ctx.piComputator.next();
            logger.info("Pi computation finished, current progress is " + ctx.getCurrentProgress());
            markStageCompleted();
        });

        if (decideWhetherToUpdateNetMap()) {
            logger.info("Decided to gather node info");
            stagesRemained++;
            sender.broadcast(new RequestForNodeInfo(), 5000,
                    (source, response) -> ctx.netmap.add(response.nodeInfo),
                    () -> {
                        logger.info("Finished gathering node info");
                        markStageCompleted();
                    });
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


    // --- passing token ---

    private void passToken() {
        assert ctx.netmap.size() != 0;
        if (ctx.netmap.size() == 1) {
            logger.info("No more nodes are known to give token");
            ctx.switchToState(new TokenHolderState(ctx));
        } else {
            acceptingTokenNode = ctx.netmap.getNextFrom(sender.getNodeInfo());
            sender.send(new InetSocketAddress(acceptingTokenNode.address, 0), new PassTokenHandshakeMsg(), DispatchType.UDP, 5000,
                    (source, response) -> passTokenStage2(response),
                    this::passTokenFail
            );
        }
    }

    private void passTokenStage2(PassTokenHandshakeResponseMsg handshakeResponse) {
        logger.info("Handshake success, passing token");
        sender.send(handshakeResponse.tcpAddress, new AcceptToken(ctx.piComputator, ctx.netmap), DispatchType.TCP, 5000,
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


    // --- reminders & responses ---

    private void onTokenHolderTimeoutExpiration(TokenHolderTimeoutExpireReminder reminder){
        sender.broadcast(new HaveTokenMsg(ctx.getCurrentPriority()));
        sender.remind(broadcastHaveTokenRF.newReminder(), TIC);
    }

    private void onHearFromOtherToken(HaveTokenMsg haveTokenMsg) {
        Priority ourPriority = ctx.getCurrentPriority();
        if (ourPriority.compareTo(haveTokenMsg.priority) < 0) {
            logger.info(Colorer.format("Detected token holder with %1`higher%` priority %s (our priority is %s)", haveTokenMsg.priority, ourPriority));
            ctx.switchToState(new WaiterState(ctx));
        }
    }

    private void onIdleTimeoutExpiration(IdlingTimeoutExpiredReminder reminder) {
        logger.info("Idle period passed");
        markStageCompleted();
    }

}
