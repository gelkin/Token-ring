import token.ring.NodeContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws IOException {
//        Collections.list(NetworkInterface.getNetworkInterfaces())
//                .forEach(System.out::println);

        try (NodeContext nodeContext = new NodeContext(NetworkInterface.getByName("wlan0"), 1247)) {
            nodeContext.initiate();

            readCommands(nodeContext);
        }
    }

    private static void readCommands(NodeContext nodeCtx) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        String line;
        while ((line = in.readLine()) != null) {
            if ("stop".equals(line))
                break;


            Matcher matcher = Pattern.compile("port (\\d+)").matcher(line);
            if (matcher.matches()) {
                try {
                    int port = Integer.parseInt(matcher.group(1));
                    if (port <= 0) {
                        System.out.println("Bad port");
                        break;
                    }
                    nodeCtx.sender.changePorts(port);
                } catch (NumberFormatException e) {
                    System.out.println("Bad port");
                }
            }
        }
    }

}
