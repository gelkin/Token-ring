package token.ring;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UniqueValue implements Comparable<UniqueValue>, Serializable {
    public static final int MAC_LENGTH = 6;

    private final byte[] mac;
    private final String name;

    public UniqueValue(byte[] mac, String name) {
        this.name = name;
        if (mac.length != MAC_LENGTH)
            throw new IllegalArgumentException(String.format("Expected mac of length %d, but given has length %d", MAC_LENGTH, mac.length));

        this.mac = mac;
    }

    public static UniqueValue getLocal(NetworkInterface network) throws SocketException {
        byte[] hardwareAddress = network.getHardwareAddress();
        if (hardwareAddress == null)
            throw new IllegalArgumentException(String.format("Network %s has no hardware address", network));

        List<String> availableHostNames = Collections.list(network.getInetAddresses()).stream()
                .map(InetAddress::getHostName)
                .collect(Collectors.toList());

        Pattern pattern = Pattern.compile("[^:]*\\w[^:]*");  // is not ip address, v4 or v6
        String hostName = availableHostNames.stream()
                .filter(name -> pattern.matcher(name).matches())
                .findAny()
                .orElse(availableHostNames.stream()
                        .findAny()
                        .orElse("Anonimus"));

        return new UniqueValue(hardwareAddress, hostName);
    }

    @Override
    public String toString() {
        StringBuilder macReadable = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            macReadable.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }
        return String.format("[%s %10s]", macReadable.toString(), name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UniqueValue that = (UniqueValue) o;

        return Arrays.equals(mac, that.mac);

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mac);
    }

    @Override
    public int compareTo(UniqueValue o) {
        for (int i = 0; i < MAC_LENGTH; i++) {
            int cmp = Byte.compare(mac[i], o.mac[i]);
            if (cmp != 0)
                return cmp;
        }
        return 0;
    }


}
