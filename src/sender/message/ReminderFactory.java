package sender.message;

import misc.Colorer;
import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

/**
 * Allows to create unique reminder messages.
 * <p>
 * When several different reminders received, only the last sent one created with this factory will act.
 */
public abstract class ReminderFactory<R extends ReminderMessage> implements ReplyProtocol<R, VoidMessage> {
    private static final Logger logger = Logger.getLogger(ReminderFactory.class);

    private static final AtomicLong factoriesCreated = new AtomicLong();

    private final BiFunction<Long, Long, R> reminderCtor;

    private final long factoryId = factoriesCreated.getAndIncrement();
    private final AtomicLong lastReminderId = new AtomicLong();

    public ReminderFactory(BiFunction<Long, Long, R> reminderConstructor) {
        this.reminderCtor = reminderConstructor;
    }

    public R newReminder() {
        return reminderCtor.apply(factoryId, lastReminderId.incrementAndGet());
    }

    @Override
    public VoidMessage makeResponse(R r) {
        if (factoryId == r.factoryId && lastReminderId.get() == r.reminderId) {
            logger.info(Colorer.paint("!!", Colorer.Format.GREEN) + " Got reminder: " + r);
            onRemind(r);
        }
        return null;
    }

    protected abstract void onRemind(R reminder);
}
