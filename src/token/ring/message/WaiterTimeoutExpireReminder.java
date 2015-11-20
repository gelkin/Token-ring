package token.ring.message;

import sender.message.ReminderIdentifier;
import sender.message.ReminderMessage;

public class WaiterTimeoutExpireReminder extends ReminderMessage {
    public WaiterTimeoutExpireReminder(ReminderIdentifier id) {
        super(id);
    }
}
