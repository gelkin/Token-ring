package sender.main;

import misc.Colorer;
import org.apache.log4j.Logger;
import token.ring.UniqueValue;
import sender.connection.*;
import sender.listeners.Cancellation;
import sender.listeners.ReceiveListener;
import sender.listeners.ReplyProtocol;
import sender.listeners.TimeoutListener;
import sender.message.MessageIdentifier;
import sender.message.ReminderMessage;
import sender.util.Serializer;
import sender.util.StreamUtil;
import token.ring.NodeInfo;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * API for Replica (lower) layer.
 */
public class MessageSender implements Closeable {
    private static Logger logger = Logger.getLogger(MessageSender.class);

    private ExecutorService executor = Executors.newCachedThreadPool();
    private ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private final BlockingQueue<Message> received = new LinkedBlockingQueue<>();

    private final TcpListener tcpListener;
    private final UdpListener udpListener;
    private final NetDispatcher tcpDispatcher;
    private final NetDispatcher udpDispatcher;

    private final UniqueValue unique;
    private final InetAddress listeningAddress;

    private final Serializer serializer = new Serializer();

    private final Semaphore freezeControl = new Semaphore(1);

    private final Collection<ReplyProtocol> replyProtocols = new ConcurrentLinkedQueue<>();
    private final Map<MessageIdentifier, Consumer<ResponseMessage>> responsesWaiters = new ConcurrentHashMap<>();
    private final BlockingQueue<Runnable> toProcess = new LinkedBlockingQueue<>();

    public MessageSender(NetworkInterface networkInterface, int udpPort) throws IOException {
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        if (!inetAddresses.hasMoreElements())
            throw new IllegalArgumentException(String.format("Network interface %s has no inet addresses", networkInterface));
        this.listeningAddress = inetAddresses.nextElement();
        this.unique = UniqueValue.getLocal(networkInterface);

        logger.info(Colorer.format("Initiating", Colorer.Format.PLAIN));
        printLegend();
        freeze();
        executor.submit(udpListener = new UdpListener(udpPort, this::acceptMessage));
        executor.submit(tcpListener = new TcpListener(this::acceptMessage));
        executor.submit(udpDispatcher = new UdpDispatcher(udpPort));
        executor.submit(tcpDispatcher = new TcpDispatcher());
        executor.submit(new IncomeMessagesProcessor());
        executor.submit(new MainProcessor());
    }

    private void printLegend() {
        logger.info(Colorer.paint("----------------------------------------------------------", Colorer.Format.BLACK));
        logger.info("Sender legend:");
        logger.info(String.format("%s for sending UDP", ColoredArrows.UDP));
        logger.info(String.format("%s for sending UDP broadcasts", ColoredArrows.UDP_BROADCAST));
        logger.info(String.format("%s for sending TCP", ColoredArrows.TCP));
        logger.info(String.format("%s for sending to self", ColoredArrows.LOOPBACK));
        logger.info(String.format("%s for received", ColoredArrows.RECEIVED));
        logger.info(String.format("%s for imitating message received again", ColoredArrows.RERECEIVE));
        logger.info(Colorer.paint("----------------------------------------------------------", Colorer.Format.BLACK));
//        logger.info("----------------------------------------------------------");
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
            ReplyType response = submit(address, message, type, timeout)
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
     * But no two response-actions (onReceive or onTimeout on any request) or response protocols will be executed at same time,
     * so you can write not thread-safe code inside them.
     * <p>
     * You may not specify port when sending UDP
     *
     * @param address         receiver of message
     * @param message         mail entry
     * @param type            way of sending a message: TCP, single UPD...
     * @param timeout         timeout in milliseconds
     * @param receiveListener an action to invoke when got an answer
     * @param timeoutListener an action to invoke when timeout exceeded and no message has been received
     * @param <ReplyType>     response message type
     */
    public <ReplyType extends ResponseMessage> void send(
            InetSocketAddress address,
            RequestMessage<ReplyType> message,
            DispatchType type,
            int timeout,
            ReceiveListener<ReplyType> receiveListener,
            TimeoutListener timeoutListener
    ) {
        // 0 for idling, -1 for fail, 1 for received
        AtomicInteger ok = new AtomicInteger();

        submit(address, message, type, timeout, response -> {
            if (ok.compareAndSet(0, 1)) {
                toProcess.offer(() -> receiveListener.onReceive(address, response));
            }
        });

        scheduledExecutor.schedule(() -> {
            if (ok.compareAndSet(0, -1)) {
                toProcess.offer(timeoutListener::onTimeout);
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
     * But no two response-actions (onReceive or onTimeout on any request) or response protocols will be executed at same time,
     * so you can write not thread-safe code inside them.
     *
     * @param message         mail entry
     * @param timeout         timeout in milliseconds
     * @param receiveListener is executed when get a response
     * @param onTimeout       is executed when timeout expires. No receiveListener will be invoked after this.
     *                        Note that this listener is invoked even if no message has been received
     * @param <ReplyType>     response type
     */
    public <ReplyType extends ResponseMessage> void broadcast(RequestMessage<ReplyType> message, int timeout, ReceiveListener<ReplyType> receiveListener, TimeoutListener onTimeout) {
        AtomicBoolean timeoutExpired = new AtomicBoolean();

        submit(null, message, DispatchType.UDP, timeout, response -> {
            if (timeoutExpired.get()) return;
            toProcess.offer(() -> receiveListener.onReceive(message.getResponseListenerAddress(), response));
        });

        scheduledExecutor.schedule(() -> {
            timeoutExpired.set(true);
            toProcess.offer(onTimeout::onTimeout);
        }, timeout, TimeUnit.MILLISECONDS);
    }

    public <ReplyType extends ResponseMessage> void broadcast(RequestMessage<ReplyType> message, int timeout, ReceiveListener<ReplyType> receiveListener) {
        broadcast(message, timeout, receiveListener, () -> {
        });
    }

    public <ReplyType extends ResponseMessage> void broadcast(RequestMessage<ReplyType> message, int timeout) {
        broadcast(message, timeout, (source, response) -> {
        });
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

    public void rereceive(RequestMessage message) {
        logger.info(ColoredArrows.RECEIVED + String.format(" %s", message));
        received.offer(message);
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
            logger.info(ColoredArrows.LOOPBACK + String.format(" %s", message));
            received.offer(message);
            return;
        }

        if (dispatchType == DispatchType.UDP) {
            if (address == null) {
                logger.info(ColoredArrows.UDP_BROADCAST + String.format(" %s", message));
            } else {
                logger.info(ColoredArrows.UDP + String.format(" %s: %s", address, message));
            }
            udpDispatcher.send(toSendableForm(address, message));
        } else if (dispatchType == DispatchType.TCP) {
            logger.info(ColoredArrows.TCP + String.format(" %s: %s", address, message));
            tcpDispatcher.send(toSendableForm(address, message));
        } else {
            throw new IllegalArgumentException("Can't process dispatch type of " + dispatchType);
        }
    }

    private SendInfo toSendableForm(InetSocketAddress address, Message message) {
        return new SendInfo(address, serializer.serialize(message));
    }

    private void acceptMessage(byte[] bytes) {
        try {
            Message message = (Message) serializer.deserialize(bytes);
            if (message.getIdentifier().unique.equals(unique))
                return;  // skip if sent by itself; loopback messages are put to queue directly and hence not missed

            received.offer(message);
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
     * methods
     * <p>
     * Caution: this method MUST be invoked inside response-action, in order to avoid unexpected concurrent effects
     */
    public void freeze() {
        freezeControl.acquireUninterruptibly();
        logger.info(Colorer.paint("***", Colorer.Format.BLUE) + " Freeze");

        replyProtocols.clear();
        responsesWaiters.clear();
        toProcess.clear();
    }

    /**
     * Unfreezes request-messages receiver. Messages received in frozen state begin to be processed.
     */
    public void unfreeze() {
        logger.info(Colorer.paint("&&&", Colorer.Format.CYAN) + " Defrost");
        freezeControl.release();
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

    public NodeInfo getNodeInfo() {
        return new NodeInfo(listeningAddress, tcpListener.getListeningPort(), unique);
    }

    @Override
    public void close() throws IOException {
        logger.info(Colorer.paint("Shutdown", Colorer.Format.PLAIN));
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
                    // if in frozen state, wait for unfreezing
                    freezeControl.acquire();
                    freezeControl.release();

                    Message message = received.take();
                    if (!(message instanceof ReminderMessage))
                        logger.info(ColoredArrows.RECEIVED + String.format(" [%s] %s", message.getIdentifier().unique, message));

                    if (message instanceof RequestMessage)
                        process(((RequestMessage) message));
                    else if (message instanceof ResponseMessage)
                        toProcess.offer(() -> process((ResponseMessage) message));
                    else
                        logger.warn("Got message of unknown type: " + message.getClass().getSimpleName());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void process(RequestMessage request) {
            for (ReplyProtocol replyProtocol : replyProtocols) {
                try {
                    ResponseMessage response = tryExecuteProtocol(replyProtocol, request);
                    if (response != null) {
                        response.setIdentifier(request.getIdentifier());
                        forwardSingle(request.getResponseListenerAddress(), response, DispatchType.UDP);
                    }
                    return;
                } catch (ClassCastException ignored) {
                }
            }
            logger.trace(Colorer.format("%1`(ignored)%` %s", request));
        }

        private <Q extends RequestMessage<A>, A extends ResponseMessage> A tryExecuteProtocol(ReplyProtocol<Q, A> replyProtocol, Q message) {
            return replyProtocol.makeResponse(message);
        }

        private void process(ResponseMessage message) {
            Consumer<ResponseMessage> responseWaiter = responsesWaiters.get(message.getIdentifier());
            if (responseWaiter != null) {
                responseWaiter.accept(message);
            }  // otherwise it has been removed due to timeout expiration
        }
    }

    private class MainProcessor implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    toProcess.take().run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private enum ColoredArrows {
        UDP_BROADCAST(Colorer.format("--%4`>%`--%4`>>%`--%4`>>%`")),
        UDP(Colorer.format("--%4`>%`--%4`>%`--%4`>%`")),
        TCP(Colorer.format("--%1`>%`--%1`>%`--%1`>%`")),
        LOOPBACK(Colorer.format("-%5`>%`-----%5`<%`-")),
        RECEIVED(Colorer.format("%3`<%`--%3`<%`--%3`<%`--")),
        RERECEIVE(Colorer.format("-%6`<%`-------"));

        private final String text;

        ColoredArrows(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
