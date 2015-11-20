package sender.main;

import sender.message.MessageIdentifier;

public class RequestMessage<ReplyType extends ResponseMessage> extends Message {
    private static final long serialVersionUID = 524448056497362233L;

    @Override
    final public MessageIdentifier getIdentifier() {
        return super.getIdentifier();
    }
}
