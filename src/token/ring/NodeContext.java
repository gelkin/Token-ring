package token.ring;

import computation.HexPiComputation;
import misc.Colorer;
import org.apache.log4j.Logger;
import sender.main.MessageSender;
import token.ring.states.WaiterState;

import java.io.Closeable;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NodeContext implements Closeable {
    private static final Logger logger = Logger.getLogger(NodeContext.class);

    public int PI_PRECISION_STEP = 20;

    public NodeState currentState;

    public MessageSender sender;
    public NetworkMap netmap;

    public HexPiComputation piComputator = new HexPiComputation(PI_PRECISION_STEP);
    public final ExecutorService executor = Executors.newCachedThreadPool();

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
        return new Priority(getCurrentProgress(), sender.getUnique());
    }

    public int getCurrentProgress() {
        return piComputator.getCurrentPrecision();
    }

    @Override
    public void close() {
        try {
            sender.close();
        } catch (IOException ignored) {
        }
    }
}
