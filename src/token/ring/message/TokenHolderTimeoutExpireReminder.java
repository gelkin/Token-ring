package token.ring.message;

import sender.message.ReminderMessage;

public class TokenHolderTimeoutExpireReminder extends ReminderMessage {
    public TokenHolderTimeoutExpireReminder(long factoryId, long reminderId) {
        super(factoryId, reminderId);
    }
}
