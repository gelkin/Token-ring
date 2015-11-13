package sender.main;

import java.util.Collection;
import java.util.concurrent.*;

class Scheduler {
    private ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private Collection<ScheduledFuture<?>> scheduledTasks = new ConcurrentLinkedQueue<>();

    public void schedule(long timeout, Runnable command) {
        ScheduledFuture<?> scheduled = scheduledExecutor.schedule(command, timeout, TimeUnit.MILLISECONDS);
        scheduledTasks.add(scheduled);
    }

    public void abort() {
        Collection<ScheduledFuture<?>> scheduled = this.scheduledTasks;
        scheduledTasks = new ConcurrentLinkedQueue<>();
        scheduled.forEach(item -> item.cancel(false));
    }

    public void shutdownNow() {
        scheduledExecutor.shutdownNow();
    }
}
