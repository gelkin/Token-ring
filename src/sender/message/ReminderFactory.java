package sender.message;

import misc.Colorer;
import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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

    private final Class<R> reminderType;

    public ReminderFactory(BiFunction<Long, Long, R> reminderConstructor) {
        this.reminderCtor = reminderConstructor;

        R apply = reminderConstructor.apply(-1L, -1L);
        //noinspection unchecked
        this.reminderType = (Class<R>) apply.getClass();
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

    @Override
    public final Class<? extends R> requestType() {
        return reminderType;
    }

    public static <R extends ReminderMessage> ReminderFactory of(BiFunction<Long, Long, R> reminderConstructor, Consumer<R> onRemind) {
        return new ReminderFactory<R>(reminderConstructor) {
            @Override
            protected void onRemind(R reminder) {
                onRemind.accept(reminder);
            }
        };
    }

}
