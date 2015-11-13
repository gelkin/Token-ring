package token.ring.message;

import sender.message.ReminderMessage;

public class LostTokenTimeoutExpireReminder extends ReminderMessage {
    public LostTokenTimeoutExpireReminder(long factoryId, long reminderId) {
        super(factoryId, reminderId);
    }
}
