package token.ring.message;

import sender.message.ReminderIdentifier;
import sender.message.ReminderMessage;

public class IdlingTimeoutExpiredReminder extends ReminderMessage {
    public IdlingTimeoutExpiredReminder(ReminderIdentifier id) {
        super(id);
    }
}
