package sender.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {
    public static <T> Stream<T> fromBlockingQueue(BlockingQueue<T> queue, int timeout) {
        Iterable<T> it = () -> new BlockingQueueIterator<>(queue, timeout);
        return StreamSupport.stream(it.spliterator(), false);
    }

    private static class BlockingQueueIterator<T> implements Iterator<T> {
        private final BlockingQueue<T> queue;
        private final long stopTime;

        private T nextElement;

        public BlockingQueueIterator(BlockingQueue<T> queue, long timeout) {
            this.queue = queue;
            this.stopTime = System.currentTimeMillis() + timeout;
        }

        @Override
        public boolean hasNext() {
            return acquireNext() != null;
        }

        @Override
        public T next() {
            return Optional.ofNullable(acquireNext())
                    .orElseThrow(IllegalStateException::new);
        }

        private T acquireNext() {
            if (nextElement != null) {
                long remainingTime = stopTime - System.currentTimeMillis();
                if (remainingTime <= 0)
                    return null;

                try {
                    nextElement = queue.poll(remainingTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            return nextElement;
        }
    }
}
