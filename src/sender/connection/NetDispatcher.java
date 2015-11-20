package sender.connection;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class NetDispatcher implements Runnable {
    protected final Logger logger = Logger.getLogger(this.getClass());

    private final BlockingQueue<SendInfo> queue = new LinkedBlockingQueue<>();

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                SendInfo sendInfo = queue.take();
                try {
                    submit(sendInfo);
                } catch (IOException e) {
                    logger.info(String.format("Dispatch failure (to %s, %s)", sendInfo.address, e.getMessage()));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected abstract void submit(SendInfo sendInfo) throws IOException;

    public void send(SendInfo sendInfo) {
        queue.offer(sendInfo);
    }

}
