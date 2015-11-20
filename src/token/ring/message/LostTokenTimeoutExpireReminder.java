package token.ring.message;

import sender.message.ReminderIdentifier;
import sender.message.ReminderMessage;

public class LostTokenTimeoutExpireReminder extends ReminderMessage {
    public LostTokenTimeoutExpireReminder(ReminderIdentifier id) {
        super(id);
    }
}
