package token.ring;

import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import sender.main.RequestMessage;
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
                new FromTokenHolder2()
        };
    }

    private void reactOnRequestFromToken(RequestMessage requestFromToken) {
        // delegate processing to waiter
        ctx.switchToState(new WaiterState(ctx));
        ctx.sender.rereceive(requestFromToken);
    }

    private class FromTokenHolder0 implements ReplyProtocol<HaveTokenMsg, VoidMessage> {
        @Override
        public VoidMessage makeResponse(HaveTokenMsg haveTokenMsg) {
            reactOnRequestFromToken(haveTokenMsg);
            return null;
        }

        @Override
        public Class<? extends HaveTokenMsg> requestType() {
            return HaveTokenMsg.class;
        }
    }

    private class FromTokenHolder1 implements ReplyProtocol<RequestForNodeInfo, MyNodeInfoMsg> {
        @Override
        public MyNodeInfoMsg makeResponse(RequestForNodeInfo requestForNodeInfo) {
            reactOnRequestFromToken(requestForNodeInfo);
            return null;
        }

        @Override
        public Class<? extends RequestForNodeInfo> requestType() {
            return RequestForNodeInfo.class;
        }
    }


    private class FromTokenHolder2 implements ReplyProtocol<PassTokenHandshakeMsg, PassTokenHandshakeResponseMsg> {
        @Override
        public PassTokenHandshakeResponseMsg makeResponse(PassTokenHandshakeMsg passTokenHandshakeMsg) {
            reactOnRequestFromToken(passTokenHandshakeMsg);
            return null;
        }

        @Override
        public Class<? extends PassTokenHandshakeMsg> requestType() {
            return PassTokenHandshakeMsg.class;
        }
    }

}
