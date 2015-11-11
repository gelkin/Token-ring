package sender.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class UdpDispatcher extends NetDispatcher {
    private final InetSocketAddress broadcastAddress;

    public UdpDispatcher(int broadcastPort) {
        broadcastAddress = new InetSocketAddress("255.255.255.255", broadcastPort);
    }

    @Override
    protected void submit(SendInfo sendInfo) {
        try {
            InetSocketAddress address = sendInfo.address;
            if (address == null) {
                address = broadcastAddress;
            }

            DatagramSocket socket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(sendInfo.data, sendInfo.data.length, address);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
