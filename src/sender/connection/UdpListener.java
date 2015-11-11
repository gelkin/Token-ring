package sender.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.function.Consumer;

public class UdpListener extends NetListener<DatagramSocket> {
    public UdpListener(int port, Consumer<byte[]> dataConsumer) throws IOException {
        super(port, dataConsumer);
    }

    @Override
    protected DatagramSocket createSocket(int port) throws SocketException {
        return new DatagramSocket(port);
    }

    @Override
    protected byte[] receive(DatagramSocket socket) throws IOException {
        byte[] bytes = new byte[1500];
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        socket.receive(packet);

        return Arrays.copyOf(packet.getData(), packet.getLength());
    }

    public InetSocketAddress getListeningAddress() {
        return new InetSocketAddress(getSocket().getLocalAddress(), getSocket().getLocalPort());
    }
}
