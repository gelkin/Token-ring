package token.ring.message;

import sender.message.ReminderMessage;

public class WaiterTimeoutExpireReminder extends ReminderMessage {
    public WaiterTimeoutExpireReminder(long factoryId, long reminderId) {
        super(factoryId, reminderId);
    }
}
