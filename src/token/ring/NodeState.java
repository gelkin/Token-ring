package token.ring;

import sender.main.MessageSender;

public abstract class NodeState {

    protected final NodeContext ctx;
    protected final MessageSender sender;

    public NodeState(NodeContext ctx) {
        this.ctx = ctx;
        this.sender = ctx.sender;
    }

    public abstract void start();

    public void close() {
    }
}
