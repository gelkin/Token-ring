package token.ring;

import computation.HexPiComputation;
import org.apache.log4j.Logger;
import sender.main.MessageSender;
import token.ring.states.WaiterState;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NodeContext implements Closeable {
    private static final Logger logger = Logger.getLogger(NodeContext.class);
    public static final String TIMEOUT_PROPERTIES_FILE = "timeouts.properties";

    public int PI_PRECISION_STEP = 20;

    public NodeState currentState;

    public MessageSender sender;
    public NetworkMap netmap;

    public HexPiComputation piComputator = new HexPiComputation(PI_PRECISION_STEP);
    public final ExecutorService executor = Executors.newCachedThreadPool();

//    public final Visualizer visualizer = new Visualizer();

    private final Properties timeouts = new Properties();

    public NodeContext(NetworkInterface networkInterface, int udpPort) throws IOException {
        try (FileInputStream propIn = new FileInputStream(TIMEOUT_PROPERTIES_FILE)) {
            timeouts.load(propIn);
        }
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

//        logger.info(Colorer.format("%2`$ -> $ %` State changed: %s -> %s", currentState.getClass().getSimpleName(), newState.getClass().getSimpleName()));
//        tellToVisualizer(newState);

        currentState = newState;
        currentState.start();
        sender.unfreeze();
    }

//    private void tellToVisualizer(NodeState newState) {
//        sender.broadcast(new ChangedStateLogMsg(Visualizer.State.of(newState)));
//        visualizer.getReplyProtocols().forEach(sender::registerReplyProtocol);
//    }

    public Priority getCurrentPriority() {
        return new Priority(piComputator.getCurrentPrecision(), sender.getUnique());
    }

    public int getTimeout(String propertyName) {
        String timeout = timeouts.getProperty(propertyName);
        if (timeout == null)
            throw new IllegalArgumentException(String.format("No timeout %s found", propertyName));
        int answer = Integer.parseInt(timeout);

        String multiplier = timeouts.getProperty("multiplier");
        if (multiplier != null) {
            answer = ((int) (answer * Double.parseDouble(multiplier)));
        }
        return answer;
    }


    @Override
    public void close() {
        try {
            sender.close();
        } catch (IOException ignored) {
        }
    }
}
