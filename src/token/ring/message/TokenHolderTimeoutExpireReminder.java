package token.ring.message;

import sender.message.ReminderIdentifier;
import sender.message.ReminderMessage;

public class TokenHolderTimeoutExpireReminder extends ReminderMessage {
    public TokenHolderTimeoutExpireReminder(ReminderIdentifier id) {
        super(id);
    }
}
