package sender.message;

import misc.Colorer;
import org.apache.log4j.Logger;
import sender.listeners.ReplyProtocol;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Allows to create unique reminder messages.
 * <p>
 * When several different reminders received, only the last sent one created with this factory will act.
 */
public abstract class ReminderFactory<R extends ReminderMessage> implements ReplyProtocol<R, VoidMessage> {
    private static final Logger logger = Logger.getLogger(ReminderFactory.class);

    private static final AtomicLong factoriesCreated = new AtomicLong();

    private final Function<ReminderIdentifier, R> reminderCtor;

    private final long factoryId = factoriesCreated.getAndIncrement();
    private final AtomicLong lastReminderId = new AtomicLong();

    private final Class<R> reminderType;

    public ReminderFactory(Function<ReminderIdentifier, R> reminderConstructor) {
        this.reminderCtor = reminderConstructor;

        R apply = reminderConstructor.apply(new ReminderIdentifier(-1, -1));
        //noinspection unchecked
        this.reminderType = (Class<R>) apply.getClass();
    }

    public R newReminder() {
        ReminderIdentifier id = new ReminderIdentifier(factoryId, lastReminderId.incrementAndGet());
        return reminderCtor.apply(id);
    }

    @Override
    public VoidMessage makeResponse(R r) {
        if (factoryId == r.getId().factoryId && lastReminderId.get() == r.getId().reminderId) {
            logger.info(Colorer.paint("!!", Colorer.Format.MAGENTA) + " Got reminder: " + r);
            onRemind(r);
        }
        return null;
    }

    protected abstract void onRemind(R reminder);

    @Override
    public final Class<? extends R> requestType() {
        return reminderType;
    }

    public static <R extends ReminderMessage> ReminderFactory of(Function<ReminderIdentifier, R> reminderConstructor, Consumer<R> onRemind) {
        return new ReminderFactory<R>(reminderConstructor) {
            @Override
            protected void onRemind(R reminder) {
                onRemind.accept(reminder);
            }
        };
    }

}
