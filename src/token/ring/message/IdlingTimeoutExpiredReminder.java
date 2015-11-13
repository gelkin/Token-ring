package token.ring.message;

import sender.message.ReminderMessage;

public class IdlingTimeoutExpiredReminder extends ReminderMessage {
    public IdlingTimeoutExpiredReminder(long factoryId, long reminderId) {
        super(factoryId, reminderId);
    }

}
