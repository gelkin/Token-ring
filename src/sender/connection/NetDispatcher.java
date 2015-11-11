package sender.connection;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class NetDispatcher implements Runnable {

    private final BlockingQueue<SendInfo> queue = new LinkedBlockingQueue<>();

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                submit(queue.take());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected abstract void submit(SendInfo sendInfo);

    public void send(SendInfo sendInfo) {
        queue.offer(sendInfo);
    }

}
