package token.ring.states;

import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import token.ring.NodeContext;
import token.ring.NodeState;
import token.ring.Priority;
import token.ring.ScaredOfTokenMsgs;
import token.ring.message.AmCandidateMsg;
import token.ring.message.AmCandidateResponseMsg;
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
                        new ListenToOtherCandidates()
                )
        ).forEach(sender::registerReplyProtocol);

        sender.broadcast(new AmCandidateMsg(ctx.getCurrentPriority()), 5000,
                (source, response) -> {
                    logger.info("Detected superior candidate");
                    ctx.switchToState(new WaiterState(ctx));
                },
                () -> {
                    logger.info("No more appropriate candidates");
                    ctx.switchToState(new TokenHolderState(ctx));
                }
        );
    }

    private class ListenToOtherCandidates implements ReplyProtocol<AmCandidateMsg, AmCandidateResponseMsg>{
        @Override
        public AmSuperiorCandidateMsg makeResponse(AmCandidateMsg otherCandidateClaim) {
            Priority outPriority = ctx.getCurrentPriority();
            int isHisGreater = otherCandidateClaim.priority.compareTo(outPriority);
            if (isHisGreater > 0) {
                logger.info(String.format("Detected Candidate with higher priority %s (our priority is %s)", otherCandidateClaim.priority, outPriority));
                ctx.switchToState(new WaiterState(ctx));
                return null;
            } else if (isHisGreater == 0) {
                logger.error("WTF? Heard the same priority from another candidate!");
            } else {
                logger.info(String.format("Detected Candidate with lower priority %s (our priority is %s)", otherCandidateClaim.priority, outPriority));
            }

            // tell him that he is not a nice guy
            // in case he missed our proposal at election
            return new AmSuperiorCandidateMsg();
        }

        @Override
        public Class<? extends AmCandidateMsg> requestType() {
            return AmCandidateMsg.class;
        }
    }

}
