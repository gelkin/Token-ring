package sender.connection;

import java.io.IOException;
import java.net.*;
import java.util.Objects;
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
                .map(InterfaceAddress::getBroadcast)
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Network interface has no broadcast addresses"));
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

    public void changePort(int newPort) {
        changePort(newPort);
    }
}
