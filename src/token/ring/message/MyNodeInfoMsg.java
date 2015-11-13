package token.ring.message;

import sender.main.ResponseMessage;
import token.ring.NodeInfo;

public class MyNodeInfoMsg extends ResponseMessage {
    public final NodeInfo nodeInfo;

    public MyNodeInfoMsg(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }
}
