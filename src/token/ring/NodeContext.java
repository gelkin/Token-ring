package token.ring;

import misc.Colorer;
import org.apache.log4j.Logger;
import sender.main.MessageSender;
import token.ring.states.WaiterState;

import java.io.Closeable;
import java.io.IOException;
import java.net.NetworkInterface;


public class NodeContext implements Closeable {
    private static final Logger logger = Logger.getLogger(NodeContext.class);

    public NodeState currentState;

    public MessageSender sender;
    public NetworkMap netmap;

    public NodeContext(NetworkInterface networkInterface, int udpPort) throws IOException {
        sender = new MessageSender(networkInterface, udpPort);
        netmap = new NetworkMap(sender.getNodeInfo());
        currentState = new WaiterState(this);
    }

    public void initiate() {
        currentState.start();
        sender.unfreeze();
    }

    public void switchToState(NodeState newState) {
        sender.freeze();
        newState.close();
        logger.info(Colorer.format("%2`$ -> $ %` State changed: %s -> %s", currentState.getClass().getSimpleName(), newState.getClass().getSimpleName()));

        currentState = newState;
        sender.unfreeze();
        currentState.start();
    }

    public Priority getCurrentPriority() {
        //TODO: computation
        return null;
    }

    @Override
    public void close() {
        try {
            sender.close();
        } catch (IOException ignored) {
        }
    }
}
