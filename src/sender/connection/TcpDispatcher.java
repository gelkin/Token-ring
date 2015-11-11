package sender.connection;

import java.io.IOException;
import java.net.Socket;

public class TcpDispatcher extends NetDispatcher {

    @Override
    protected void submit(SendInfo sendInfo) throws IOException {
        try (Socket socket = new Socket(sendInfo.address.getAddress(), sendInfo.address.getPort())) {
            socket.getOutputStream()
                    .write(sendInfo.data);
        }
    }
}
