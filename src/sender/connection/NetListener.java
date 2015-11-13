package sender.connection;


import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

public abstract class NetListener<S extends Closeable> implements Runnable, Closeable {
    protected final Logger logger = Logger.getLogger(this.getClass());

    public static final int RESTORE_ATTEMPTS_DELAY = 5000;

    private final int port;
    private S socket;

    private final Consumer<byte[]> dataConsumer;

    private boolean isClosed;

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
                    if (data != null) {
                        dataConsumer.accept(data);
                    }
                } catch (IOException e) {
                    if (!isClosed) {
                        logger.info("Data sending failed, initiating restore protocol");
                        restore();
                    }
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
                logger.info("Listener restored");
            } catch (IOException e) {
                if (isClosed)
                    return;

                logger.info(String.format("Restore was unsuccessful. Repeat in %d ms.", RESTORE_ATTEMPTS_DELAY));
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
        isClosed = true;
        socket.close();
    }

    public int getListeningPort() {
        return port;
    }
}
