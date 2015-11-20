package token.ring;

import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import sender.main.RequestMessage;
import token.ring.message.AcceptToken;
import token.ring.message.HaveTokenMsg;
import token.ring.message.PassTokenHandshakeMsg;
import token.ring.message.RequestForNodeInfo;
import token.ring.states.WaiterState;

import java.util.stream.Stream;

public class ScaredOfTokenMsgs {
    private final NodeContext ctx;
    private final Logger logger;

    public ScaredOfTokenMsgs(NodeContext ctx, Logger logger) {
        this.ctx = ctx;
        this.logger = logger;
    }


    public Stream<ReplyProtocol> getProtocols() {
        return Stream.<Class<? extends RequestMessage>>of(
                // request types, processing of which we would delegate to waiter
                HaveTokenMsg.class,
                RequestForNodeInfo.class,
                PassTokenHandshakeMsg.class,
                AcceptToken.class
        ).map(requestType -> ReplyProtocol.dumbOn(requestType, this::reactOnRequestFromToken));
    }

    private void reactOnRequestFromToken(RequestMessage requestFromToken) {
        logger.info("Received message from token, delegating it to waiter");

        ctx.switchToState(new WaiterState(ctx));
        ctx.sender.rereceive(requestFromToken);
    }

}
