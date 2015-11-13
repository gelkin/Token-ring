package token.ring;

import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import sender.main.RequestMessage;
import sender.main.ResponseMessage;
import sender.message.VoidMessage;
import token.ring.message.*;
import token.ring.states.WaiterState;

public class ScaredOfTokenMsgs {
    private final NodeContext ctx;
    private final Logger logger;

    public ScaredOfTokenMsgs(NodeContext ctx, Logger logger) {
        this.ctx = ctx;
        this.logger = logger;
    }


    public ReplyProtocol[] getProtocols() {
        return new ReplyProtocol[]{
                new FromTokenHolder0(),
                new FromTokenHolder1(),
                new FromTokenHolder2(),
                new FromTokenHolder3()
        };
    }

    private void reactOnRequestFromToken(RequestMessage requestFromToken) {
        // delegate processing to waiter
        ctx.switchToState(new WaiterState(ctx));
        ctx.sender.rereceive(requestFromToken);
    }

    private class FromTokenHolder0 extends ScareOfTokenHolder<HaveTokenMsg, VoidMessage> {
        @Override
        public Class<? extends HaveTokenMsg> requestType() {
            return HaveTokenMsg.class;
        }
    }

    private class FromTokenHolder1 extends ScareOfTokenHolder<RequestForNodeInfo,MyNodeInfoMsg> {
        @Override
        public Class<? extends RequestForNodeInfo> requestType() {
            return RequestForNodeInfo.class;
        }
    }


    private class FromTokenHolder2 extends ScareOfTokenHolder<PassTokenHandshakeMsg,PassTokenHandshakeResponseMsg> {
        @Override
        public Class<? extends PassTokenHandshakeMsg> requestType() {
            return PassTokenHandshakeMsg.class;
        }
    }


    private class FromTokenHolder3 extends ScareOfTokenHolder<AcceptToken,AcceptTokenResponse> {
        @Override
        public Class<? extends AcceptToken> requestType() {
            return AcceptToken.class;
        }
    }

    private abstract class ScareOfTokenHolder<Q extends RequestMessage<A>, A extends ResponseMessage> implements ReplyProtocol<Q, A> {
        @Override
        public A makeResponse(Q q) {
            reactOnRequestFromToken(q);
            return null;
        }
    }

}
