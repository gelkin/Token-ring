package sender.message;

import sender.main.RequestMessage;

public class ReminderMessage extends RequestMessage<VoidMessage> {
    final long factoryId;
    final long reminderId;

    public ReminderMessage(long factoryId, long reminderId) {
        this.factoryId = factoryId;
        this.reminderId = reminderId;
    }

    @Override
    protected boolean logOnReceive(){
        return false;
    }
}
