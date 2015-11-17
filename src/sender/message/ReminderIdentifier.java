package sender.message;

public class ReminderIdentifier {
    final long factoryId;
    final long reminderId;

    /**
     * Can be instantiated only by ReminderFactory
     */
    ReminderIdentifier(long factoryId, long reminderId) {
        this.factoryId = factoryId;
        this.reminderId = reminderId;
    }

}
