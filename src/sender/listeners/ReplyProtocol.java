package sender.listeners;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import sender.main.RequestMessage;
import sender.main.ResponseMessage;

public interface ReplyProtocol<RequestType extends RequestMessage<ReplyType>, ReplyType extends ResponseMessage> {
    @Nullable
    ReplyType makeResponse(RequestType type);

    @NotNull
    Class<? extends RequestType> requestType();

}
