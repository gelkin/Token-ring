package sender.main;

import org.apache.log4j.Logger;
import sender.*;
import sender.connection.*;
import sender.listeners.Cancellation;
import sender.listeners.FailListener;
import sender.listeners.ReceiveListener;
import sender.listeners.ReplyProtocol;
import sender.message.MessageIdentifier;
import sender.message.ReminderMessage;
import sender.util.Serializer;
import sender.util.StreamUtil;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * API for Replica (lower) layer.
 */
public class MessageSender implements Closeable {
    private static Logger logger = Logger.getLogger(MessageSender.class);

    public static final int SERVANT_THREAD_NUM = 5;

    private ExecutorService executor = Executors.newFixedThreadPool(SERVANT_THREAD_NUM);
    private ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private final BlockingQueue<Message> received = new LinkedBlockingQueue<>();

    private final TcpListener tcpListener;
    private final UdpListener udpListener;
    private final NetDispatcher tcpDispatcher;
    private final NetDispatcher udpDispatcher;

    private final UniqueValue unique;
    private final InetAddress listeningAddress;

    private final Serializer serializer = new Serializer();

    private final Lock onFrozenLock = new ReentrantLock();

    private final Collection<ReplyProtocol> replyProtocols = new ConcurrentLinkedQueue<>();
    private final Map<MessageIdentifier, Consumer<ResponseMessage>> responsesWaiters = new ConcurrentHashMap<>();

    public MessageSender(UniqueValue unique, InetAddress listeningAddress, int udpPort) throws IOException {
        this.unique = unique;
        this.listeningAddress = listeningAddress;

        freeze();
        executor.submit(udpListener = new UdpListener(udpPort, this::acceptMessage));
        executor.submit(tcpListener = new TcpListener(this::acceptMessage));
        executor.submit(udpDispatcher = new UdpDispatcher(udpPort));
        executor.submit(tcpDispatcher = new TcpDispatcher());
        executor.submit(new IncomeMessagesProcessor());
    }

    /**
     * Simply sends message, waits for result during some sensible time.
     * <p>
     * Current thread is blocked during method call.
     *
     * @param address     receiver of message
     * @param message     mail entry
     * @param type        way of sending a message: TCP, single UPD...
     * @param timeout     timeout in milliseconds
     * @param <ReplyType> response message type
     * @return response message
     * @throws SendingException when timeout exceeded
     */
    public <ReplyType extends ResponseMessage> ReplyType sendAndExpect(InetSocketAddress address, RequestMessage<ReplyType> message, DispatchType type, int timeout) throws SendingException {
        return sendAndWait(address, message, type, timeout)
                .orElseThrow(() -> new SendingException(address));
    }

    /**
     * Same as <tt>sendAndExpect</tt>, but in case of no answer returns empty Optional instead of throwing exception
     */
    public <ReplyType extends ResponseMessage> Optional<ReplyType> sendAndWait(InetSocketAddress address, RequestMessage<ReplyType> message, DispatchType type, int timeout) {
        try {
            //noinspection unchecked
            ReplyType response = (ReplyType) submit(address, message, type, timeout)
                    .poll(timeout, TimeUnit.MILLISECONDS);

            return Optional.ofNullable(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }


    /**
     * Sends a message.
     * <p>
     * Current thread is NOT blocked by this method call.
     * But no two response-actions (onReceive or onFail on any request) or response protocols will be executed at same time,
     * so you can write not thread-safe code inside them.
     * <p>
     * <p>
     * This gets being very similar to automaton programming :)
     *
     * @param address         receiver of message
     * @param message         mail entry
     * @param type            way of sending a message: TCP, single UPD...
     * @param timeout         timeout in milliseconds
     * @param receiveListener an action to invoke when got an answer.
     * @param failListener    an action to invoke when timeout exceeded.
     * @param <ReplyType>     response message type
     */
    public <ReplyType extends ResponseMessage> void send(
            InetSocketAddress address,
            RequestMessage<ReplyType> message,
            DispatchType type,
            int timeout,
            ReceiveListener<ReplyType> receiveListener,
            FailListener failListener
    ) {
        // 0 for idling, -1 for fail, 1 for received
        AtomicInteger ok = new AtomicInteger();

        submit(address, message, type, timeout, response -> {
            if (ok.compareAndSet(0, 1)) {
                receiveListener.onReceive(address, response);
            }
        });

        scheduledExecutor.schedule(() -> {
            if (ok.compareAndSet(0, -1)) {
                failListener.onFail();
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    public <ReplyType extends ResponseMessage> void send(InetSocketAddress address, RequestMessage<ReplyType> message, DispatchType type, int timeout, ReceiveListener<ReplyType> receiveListener) {
        send(address, message, type, timeout, receiveListener, () -> {
        });
    }

    /**
     * Sends a broadcast, and returns stream of answers which would be collected during timeout.
     * <p>
     * Node will receive its own request.
     * But you can detect self-sent message: response.getIdentifier().unique.equals(sender.getUnique())
     * <p>
     * Note, that this method doesn't block the thread, but accessing elements of result stream does (in lazy way).
     *
     * @param message     mail entry
     * @param timeout     timeout in milliseconds
     * @param <ReplyType> responses type
     * @return stream of replies
     */
    public <ReplyType extends ResponseMessage> Stream<ReplyType> broadcastAndWait(RequestMessage<ReplyType> message, int timeout) {
        BlockingQueue<ReplyType> responseContainer = submit(null, message, DispatchType.UDP, timeout);
        return StreamUtil.fromBlockingQueue(responseContainer, timeout);
    }

    /**
     * Sends a broadcast.
     * <p>
     * Node will receive its own request.
     * But you can detect self-sent message: response.getIdentifier().unique.equals(sender.getUnique())
     * <p>
     * Current thread is NOT blocked by this method call.
     * But no two response-actions (onReceive or onFail on any request) or response protocols will be executed at same time,
     * so you can write not thread-safe code inside them.
     *
     * @param message         mail entry
     * @param timeout         timeout in milliseconds
     * @param receiveListener is executed when get a response
     * @param <ReplyType>     response type
     */
    public <ReplyType extends ResponseMessage> void broadcast(RequestMessage<ReplyType> message, int timeout, ReceiveListener<ReplyType> receiveListener) {
        submit(null, message, DispatchType.UDP, timeout, response -> receiveListener.onReceive(message.getResponseListenerAddress(), response));
    }

    /**
     * Sends message to itself in specified delay.
     * <p>
     * Used to schedule some tasks and execute them sequentially with other response-actions.
     * Executed action must be specified as response protocol.
     *
     * @param message reminder message
     * @param delay   when to send a mention
     */
    public void remind(ReminderMessage message, int delay) {
        Runnable remindTask = () -> send(null, message, DispatchType.LOOPBACK, 10000, (addr, response) -> {
        });
        scheduledExecutor.schedule(remindTask, delay, TimeUnit.MILLISECONDS);
    }

    private <ReplyType extends ResponseMessage> BlockingQueue<ReplyType> submit(InetSocketAddress address, RequestMessage<ReplyType> message, DispatchType type, int timeout) {
        LinkedBlockingQueue<ReplyType> container = new LinkedBlockingQueue<>();
        submit(address, message, type, timeout, container::offer);
        return container;
    }

    /**
     * Puts identifier into message,
     * message into output queue,
     * reply consumer to responsesWaiters (scheduling its removal)
     */
    private <ReplyType extends ResponseMessage> void submit(InetSocketAddress address, RequestMessage<ReplyType> message, DispatchType type, int timeout, Consumer<ReplyType> consumer) {
        MessageIdentifier identifier = new MessageIdentifier(unique);
        message.setIdentifier(identifier);
        message.setResponseListenerAddress(getUdpListenerAddress());
        responsesWaiters.put(identifier, responseMessage -> {
                    try {
                        //noinspection unchecked
                        ReplyType casted = (ReplyType) responseMessage;
                        consumer.accept(casted);
                    } catch (ClassCastException e) {
                        logger.warn("Accepted message of wrong type", e);
                    }
                }
        );

        forwardSingle(address, message, type);
        scheduledExecutor.schedule(() -> responsesWaiters.remove(message.getIdentifier())
                , timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Determines behaviour on receiving request-message of specified type.
     * <p>
     * No any two response protocols or response-actions will be executed at the same time.
     *
     * @param protocol way on response on specified request-message
     * @return function to unregister this protocol.
     */
    public <Q extends RequestMessage<A>, A extends ResponseMessage> Cancellation registerReplyProtocol(ReplyProtocol<Q, A> protocol) {
        replyProtocols.add(protocol);
        return () -> replyProtocols.remove(protocol);
    }

    private void forwardSingle(InetSocketAddress address, Message message, DispatchType dispatchType) {
        if (dispatchType == DispatchType.LOOPBACK) {
            received.offer(message);
            return;
        }

        SendInfo sendInfo = toSendableForm(address, message);
        if (dispatchType == DispatchType.UDP) {
            udpDispatcher.send(sendInfo);
        } else if (dispatchType == DispatchType.TCP) {
            tcpDispatcher.send(sendInfo);
        } else {
            throw new IllegalArgumentException("Can't process dispatch type of " + dispatchType);
        }
    }

    private SendInfo toSendableForm(InetSocketAddress address, Message message) {
        return new SendInfo(address, serializer.serialize(message));
    }

    private void acceptMessage(byte[] bytes) {
        try {
            received.offer((Message) serializer.deserialize(bytes));
        } catch (IOException | ClassCastException e) {
            logger.info("Got some trash", e);
        }
    }

    /**
     * Freezes request-messages receiver.
     * <p>
     * In frozen state no any response protocol is activated, all received request-messages are stored and not processed
     * until unfreezing. So you can safely change response protocols without scaring of missing any request.
     * <p>
     * Sender is initiated in frozen state
     * <p>
     * Call of this method also destroys all registered response protocols and response-actions of send- and broadcastAndWait
     * methods (optional feature)
     */
    public void freeze() {
        onFrozenLock.lock();

        replyProtocols.clear();
        responsesWaiters.clear();
    }

    /**
     * Unfreezes request-messages receiver. Messages received in frozen state begin to be processed.
     */
    public void unfreeze() {
        onFrozenLock.unlock();
    }

    public UniqueValue getUnique() {
        return unique;
    }

    public InetSocketAddress getUdpListenerAddress() {
        return new InetSocketAddress(listeningAddress, udpListener.getListeningPort());
    }

    public InetSocketAddress getTcpListenerAddress() {
        return new InetSocketAddress(listeningAddress, tcpListener.getListeningPort());
    }

    @Override
    public void close() throws IOException {
        scheduledExecutor.shutdownNow();
        executor.shutdownNow();
        udpListener.close();
        tcpListener.close();
    }

    public enum DispatchType {
        UDP,
        TCP,
        LOOPBACK
    }

    private class IncomeMessagesProcessor implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    onFrozenLock.lock();
                    try {
                        Message message = received.take();
                        if (message instanceof RequestMessage)
                            process(((RequestMessage) message));
                        else if (message instanceof ResponseMessage)
                            process(((ResponseMessage) message));
                        else
                            logger.warn("Got message of unknown type: " + message.getClass().getSimpleName());
                    } finally {
                        onFrozenLock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void process(RequestMessage request) {
            for (ReplyProtocol replyProtocol : replyProtocols) {
                try {
                    ResponseMessage response = tryApplyProtocol(replyProtocol, request);
                    if (response != null) {
                        response.setIdentifier(request.getIdentifier());
                        forwardSingle(request.getResponseListenerAddress(), response, DispatchType.UDP);
                    }
                    return;
                } catch (ClassCastException ignored) {
                }
            }
            logger.trace(String.format("Message of type %s has been ignored", request.getClass().getSimpleName()));
        }

        private <Q extends RequestMessage<A>, A extends ResponseMessage> A tryApplyProtocol(ReplyProtocol<Q, A> replyProtocol, Q message) {
            return replyProtocol.makeResponse(message);
        }

        private void process(ResponseMessage message) {
            Consumer<ResponseMessage> responseWaiter = responsesWaiters.get(message.getIdentifier());
            if (responseWaiter != null) {
                responseWaiter.accept(message);
            }  // otherwise it has been removed due to timeout expiration
        }
    }

}
