package token.ring.states;

import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import token.ring.NodeContext;
import token.ring.NodeState;
import token.ring.Priority;
import token.ring.ScaredOfTokenMsgs;
import token.ring.message.AmCandidateMsg;
import token.ring.message.AmSuperiorCandidateMsg;

import java.util.stream.Stream;

public class CandidateState extends NodeState {
    private final static Logger logger = Logger.getLogger(CandidateState.class);

    public CandidateState(NodeContext ctx) {
        super(ctx);
    }

    @Override
    public void start() {
        Stream.concat(
                new ScaredOfTokenMsgs(ctx, logger).getProtocols(),
                Stream.of(
                        ReplyProtocol.on(AmCandidateMsg.class, this::reactOnMsgFromOtherCandidate)
                )
        ).forEach(sender::registerReplyProtocol);

        sender.broadcast(new AmCandidateMsg(ctx.getCurrentPriority()), ctx.getTimeout("candidate.main"),
                (source, response) -> {
                    response.logAboutThisMessage(logger);
                    ctx.switchToState(new WaiterState(ctx));
                },
                () -> {
                    logger.info("No more appropriate candidates");
                    ctx.switchToState(new TokenHolderState(ctx));
                }
        );
    }

    private AmSuperiorCandidateMsg reactOnMsgFromOtherCandidate(AmCandidateMsg otherCandidateClaim) {
        Priority ourPriority = ctx.getCurrentPriority();
        int isHisGreater = otherCandidateClaim.priority.compareTo(ourPriority);
        if (isHisGreater > 0) {
            logger.info(String.format("Detected Candidate with higher priority %s (our priority is %s)", otherCandidateClaim.priority, ourPriority));
            ctx.switchToState(new WaiterState(ctx));
            return null;
        } else if (isHisGreater == 0) {
            logger.error("WTF? Heard the same priority from another candidate!");
        } else {
            logger.info(String.format("Detected Candidate with lower priority %s (our priority is %s)", otherCandidateClaim.priority, ourPriority));
        }

        // tell him that he is not a nice guy
        // in case he missed our candidature
        return new AmSuperiorCandidateMsg();
    }

}
