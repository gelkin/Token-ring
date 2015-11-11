package sender.connection;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class NetDispatcher implements Runnable {

    private final BlockingQueue<SendInfo> queue = new LinkedBlockingQueue<>();

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    submit(queue.take());
                } catch (IOException e) {
                    // TODO:
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
