package sender.connection;

import java.io.IOException;
import java.net.*;
import java.util.Optional;

public class UdpDispatcher extends NetDispatcher {
    private InetAddress broadcastAddress;
    private final int port;

    public UdpDispatcher(NetworkInterface networkInterface, int udpPort) throws SocketException {
        this.port = udpPort;
        try {
            broadcastAddress = Inet4Address.getByName("255.255.255.255");
        } catch (UnknownHostException e) {
            throw new Error("Unexpected exception", e);
        }

        broadcastAddress = networkInterface.getInterfaceAddresses().stream()
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Network interface has no interface addresses"))
                .getAddress();
    }

    @Override
    protected void submit(SendInfo sendInfo) throws IOException {
        InetAddress address = Optional.ofNullable(sendInfo.address)
                .map(InetSocketAddress::getAddress)
                .orElse(broadcastAddress);

        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(sendInfo.data, sendInfo.data.length, address, port);
        socket.send(packet);
    }
}
