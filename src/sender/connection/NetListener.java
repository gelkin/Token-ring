package sender.connection;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

public abstract class NetListener<S extends Closeable> implements Runnable, Closeable {
    public static final int RESTORE_ATTEMPTS_DELAY = 1000;

    private final int port;
    private S socket;

    private final Consumer<byte[]> dataConsumer;

    public NetListener(int port, Consumer<byte[]> dataConsumer) throws IOException {
        this.port = port;
        socket = createSocket(port);
        this.dataConsumer = dataConsumer;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    byte[] data = receive(socket);
                    dataConsumer.accept(data);
                } catch (IOException e) {
                    restore();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    protected abstract byte[] receive(S socket) throws IOException;

    protected abstract S createSocket(int port) throws IOException;

    private void restore() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                try {
                    socket.close();
                } catch (Throwable ignored) {
                }
                socket = createSocket(port);
            } catch (IOException e) {
                Thread.sleep(RESTORE_ATTEMPTS_DELAY);
            }
        }
        throw new InterruptedException();
    }

    protected S getSocket() {
        return socket;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public int getListeningPort() {
        return port;
    }
}
