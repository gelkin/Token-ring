package token.ring;

import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;
import token.ring.message.logging.ChangedStateLogMsg;
import token.ring.states.CandidateState;
import token.ring.states.LostTokenState;
import token.ring.states.TokenHolderState;
import token.ring.states.WaiterState;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class Visualizer {
    private final static Logger logger = Logger.getLogger(Visualizer.class);

    private boolean ON = true;

    private final ReplyProtocol[] replyProtocols = new ReplyProtocol[]{
            ReplyProtocol.dumbOn(ChangedStateLogMsg.class, this::changeState)
    };

    private TreeMap<UniqueValue, NodeItem> nodes = new TreeMap<>();

    public Stream<ReplyProtocol> getReplyProtocols() {
        return Arrays.stream(replyProtocols);
    }

    private void changeState(ChangedStateLogMsg message) {
        logger.info("!!!!");
        UniqueValue key = message.getIdentifier().unique;
        NodeItem node = nodes.get(key);
        if (node == null)
            nodes.put(key, node = new NodeItem());
        node.state = message.newState;

        logCurrent();
    }

    private void logCurrent() {
        if (!ON)
            return;

        for (Map.Entry<UniqueValue, NodeItem> entry : nodes.entrySet()) {
            NodeItem node = entry.getValue();
            logger.info(String.format("[%s] %s", entry.getKey(), node.state));
        }
    }

    private static class NodeItem {
        public State state;
    }

    public enum State {
        TOKEN_HOLDER,
        WAITER,
        LOST_TOKEN,
        CANDIDATE;

        public static State of(NodeState state) {
            return state instanceof TokenHolderState ? TOKEN_HOLDER
                    : state instanceof WaiterState ? WAITER
                    : state instanceof LostTokenState ? LOST_TOKEN
                    : state instanceof CandidateState ? CANDIDATE
                    : null;
        }
    }
}
