package sender.listeners;

import sender.main.RequestMessage;

@FunctionalInterface
public interface FailListener<T extends RequestMessage> {
    /**
     * Action performed when expected answer has not been received.
     */
    void onFail();
}
