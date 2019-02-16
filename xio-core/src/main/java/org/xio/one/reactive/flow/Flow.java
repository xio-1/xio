/*
 * stream.java
 * Created on 14 October 2006, 10:20
 * Copyright Xio
 */
package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.domain.flow.*;
import org.xio.one.reactive.flow.domain.item.EmptyItem;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.ItemIdSequence;
import org.xio.one.reactive.flow.internal.FlowHousekeepingDaemon;
import org.xio.one.reactive.flow.internal.FlowInputMonitor;
import org.xio.one.reactive.flow.internal.FlowSubscriptionMonitor;
import org.xio.one.reactive.flow.subscribers.FunctionalStreamItemSubscriber;
import org.xio.one.reactive.flow.subscribers.FutureSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.*;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Flow
 * <p>
 * a anItemFlow stream of items
 * <p>
 * Flow is implemented with a Command Query Responsibility Segregation external objects,
 * json etc can be put into the anItemFlow and are then asynchronously loaded in memory to a
 * contents store that is used to provide a sequenced view of the flowing items to downstream
 * futureSubscriber
 *
 * @Author Richard Durley
 * @OringinalWork XIO
 * @Copyright Richard Durley / XIO.ONE
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 */
public final class Flow<T, R>
        implements Flowable<T, R>, ItemFlow<T, R>, FutureItemResultFlowable<T, R>,
        CompletableItemFlowable<T, R> {

    private static Logger logger;

    static {
        try {
            InputStream stream = Flow.class.getResourceAsStream("/logger.properties");
            if (stream != null)
                LogManager.getLogManager().readConfiguration(stream);
        } catch (Exception e) {
        } finally {
            logger = Logger.getLogger(Flow.class.getCanonicalName());
        }
    }

    public static final int LOCK_PARK_NANOS = 100000;
    public static final long DEFAULT_TIME_TO_LIVE_SECONDS = 1;
    private final int queue_max_size = 16384;

    // all flows
    private volatile static Map<String, Flow> flowMap = new ConcurrentHashMap<>();

    // streamContentsSnapShot variables
    private FlowContents<T, R> flowContents;
    // constants
    private int count_down_latch = 10;
    // input parameters
    private String name;
    private String id;
    private String indexFieldName;
    private long defaultTTLSeconds = DEFAULT_TIME_TO_LIVE_SECONDS;

    private Map<String, FlowSubscriptionTask> subscriberSubscriptions =
            new ConcurrentHashMap<>();

    // Queue control
    private BlockingQueue<Item<T, R>> item_queue;
    private volatile boolean isEnd = false;
    private volatile boolean flush = false;
    private ItemIdSequence itemIDSequence;
    private long slowDownNanos = 0;
    private boolean flushImmediately;

    //subscription control
    private ArrayList<SubscriberInterface<R, T>> subscribers;
    private final Object lockSubscriberslist = new Object();
    private FutureSubscriber<R, T> futureSubscriber = null;
    private static int flowCount = 0;
    HashMap<String, Item> lastSeenItemMap;

    private static final Object flowControlLock = new Object();

    private Flow(String name, String indexFieldName, long ttlSeconds) {
        this.id = UUID.randomUUID().toString();
        initialise(name, indexFieldName, ttlSeconds);
        synchronized (flowControlLock) {
            flowMap.put(id, this);
            flowCount++;
            if (flowCount == 1) {
                InternalExecutors.controlFlowThreadPoolInstance().submit(new FlowInputMonitor());
                InternalExecutors.controlFlowThreadPoolInstance().submit(new FlowSubscriptionMonitor());
                InternalExecutors.controlFlowThreadPoolInstance().submit(new FlowHousekeepingDaemon());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            logger.info("Flow " + name + " id " + id + " has started");
        }
    }

    private void initialise(String name, String indexFieldName, long ttlSeconds) {
        this.item_queue = new ArrayBlockingQueue<>(queue_max_size, true);
        this.name = name;
        this.indexFieldName = indexFieldName;
        if (ttlSeconds >= 0)
            this.defaultTTLSeconds = ttlSeconds;
        this.flowContents = new FlowContents<>(this);
        this.itemIDSequence = new ItemIdSequence();
        this.flushImmediately = false;
        this.subscribers = new ArrayList<>();
        this.lastSeenItemMap = new HashMap<>();
    }

    //bad use of erasure need too find a better way
    public static <T, R> ItemFlow<T, R> anItemFlow() {
        return new Flow<>(UUID.randomUUID().toString(), null, DEFAULT_TIME_TO_LIVE_SECONDS);
    }

    public static <T, R> ItemFlow<T, R> anItemFlow(String name) {
        return new Flow<>(name, null, DEFAULT_TIME_TO_LIVE_SECONDS);
    }

    public static <T, R> ItemFlow<T, R> anItemFlow(String name, long ttlSeconds) {
        return new Flow<>(name, null, ttlSeconds);
    }

    public static <T, R> ItemFlow<T, R> anItemFlow(String name, String indexFieldName) {
        return new Flow<>(name, indexFieldName, DEFAULT_TIME_TO_LIVE_SECONDS);
    }

    public static <T, R> ItemFlow<T, R> anItemFlow(String name, String indexFieldName,
                                                   long ttlSeconds) {
        return new Flow<>(name, indexFieldName, ttlSeconds);
    }

    public static <T, R> FutureItemResultFlowable<T, R> aFutureResultItemFlow(
            FutureSubscriber<R, T> futureSubscriber) {
        Flow<T, R> resultFlowable =
                new Flow<>(UUID.randomUUID().toString(), null, DEFAULT_TIME_TO_LIVE_SECONDS);
        resultFlowable.addAppropriateSubscriber(futureSubscriber);
        return resultFlowable;
    }

    public static <T, R> FutureItemResultFlowable<T, R> aFutureResultItemFlow(String name,
                                                                              FutureSubscriber<R, T> futureSubscriber) {
        Flow<T, R> resultFlowable = new Flow<>(name, null, DEFAULT_TIME_TO_LIVE_SECONDS);
        resultFlowable.addAppropriateSubscriber(futureSubscriber);
        return resultFlowable;
    }

    public static <T, R> FutureItemResultFlowable<T, R> aFutureResultItemFlow(String name,
                                                                              long ttlSeconds, FutureSubscriber<R, T> futureSubscriber) {
        Flow<T, R> resultFlowable = new Flow<>(name, null, ttlSeconds);
        resultFlowable.addAppropriateSubscriber(futureSubscriber);
        return resultFlowable;
    }

    public static <T, R> CompletableItemFlowable<T, R> aCompletableItemFlow(
            CompletableSubscriber<R, T> completableSubscriber) {
        Flow<T, R> resultFlowable =
                new Flow<>(UUID.randomUUID().toString(), null, DEFAULT_TIME_TO_LIVE_SECONDS);
        resultFlowable.addAppropriateSubscriber(completableSubscriber);
        return resultFlowable;
    }

    public static <T, R> CompletableItemFlowable<T, R> aCompletableItemFlow(String name,
                                                                            CompletableSubscriber<R, T> completableSubscriber) {
        Flow<T, R> resultFlowable = new Flow<>(name, null, DEFAULT_TIME_TO_LIVE_SECONDS);
        resultFlowable.addAppropriateSubscriber(completableSubscriber);
        return resultFlowable;
    }

    public static <T, R> CompletableItemFlowable<T, R> aCompletableItemFlow(String name,
                                                                            long ttlSeconds, CompletableSubscriber<R, T> completableSubscriber) {
        Flow<T, R> resultFlowable = new Flow<>(name, null, ttlSeconds);
        resultFlowable.addAppropriateSubscriber(completableSubscriber);
        return resultFlowable;
    }

    public static Collection<Flow> allFlows() {
        return Collections.synchronizedMap(flowMap).values();
    }

    public static Flow forID(String id) {
        return Collections.synchronizedMap(flowMap).get(id);
    }

    @Override
    public String addSubscriber(SubscriberInterface<R, T> subscriber) {
        addAppropriateSubscriber(subscriber);
        return subscriber.getId();
    }

    @Override
    public void removeSubscriber(SubscriberInterface<R, T> subscriber) {
        unsubscribe(subscriber);
    }

    @Override
    public SubscriberInterface<R, T> getSubscriber(String subscriberId) {
        if (subscriberId != null && !subscriberId.isBlank()) {
            getSubscriber(subscriberId);
        }
        throw new IllegalArgumentException("Subscriber not found");
    }

    private void addAppropriateSubscriber(SubscriberInterface<R, T> subscriber) {
        if (subscriber instanceof Subscriber)
            registerSubscriber(subscriber);
        if (subscriber instanceof FutureSubscriber)
            registerFutureSubscriber((FutureSubscriber<R, T>) subscriber);
        if (subscriber instanceof CompletableSubscriber)
            registerSubscriber(subscriber);
    }

    private void registerSubscriber(SubscriberInterface<R, T> subscriber) {
        subscribe(subscriber);
    }

    /**
     * Indicate that each item place should be flushed immediately
     * Use when low latency < 2ms is required for core
     *
     * @return
     */
    public Flowable<T, R> enableImmediateFlushing() {
        this.flushImmediately = true;
        return this;
    }

    /**
     * Return the flows unique UUID
     *
     * @return
     */
    @Override
    public String uuid() {
        return this.id;
    }

    /**
     * Return the Flow Name
     *
     * @return
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Put a putItem value into the contents
     */

    @Override
    public long putItem(T value) {
        return putItemWithTTL(defaultTTLSeconds, value)[0];
    }

    /**
     * Puts list anItemFlow values into the contents
     *
     * @param values
     * @return
     */

    @Override
    public long[] putItem(T... values) {
        return putItemWithTTL(defaultTTLSeconds, values);
    }

    /**
     * Puts list anItemFlow values into the contents with ttlSeconds
     *
     * @param values
     * @return
     */

    @Override
    public long[] putItemWithTTL(long ttlSeconds, T... values) {
        long[] ids = new long[values.length];
        if (!isEnd) {
            for (int i = 0; i < values.length; i++) {
                Item<T, R> item = new Item<>(values[i], itemIDSequence.getNext(), ttlSeconds);
                ids[i] = item.itemId();
                addToStreamWithBlock(item, flushImmediately);
                if (slowDownNanos > 0)
                    LockSupport.parkNanos(slowDownNanos);
            }
        }
        return ids;
    }

    /**
     * Submit an item to be processed by the FutureSubscriber and completionHandler to the completion handler
     *
     * @param value
     * @param completionHandler
     */
    @Override
    public void submitItem(T value, FlowItemCompletionHandler<R, T> completionHandler) {
        submitItemWithTTL(this.defaultTTLSeconds, value, completionHandler);
    }

    /**
     * Put's an item with the given futureSubscriber and completion handler
     *
     * @param value
     * @param completionHandler
     */
    @Override
    public void submitItemWithTTL(long ttlSeconds, T value,
                                  FlowItemCompletionHandler<R, T> completionHandler) {
        Item<T, R> item = new Item<>(value, itemIDSequence.getNext(), ttlSeconds, completionHandler);
        addToStreamWithBlock(item, flushImmediately);
    }

    /**
     * Each putItem is processed as a Future<R> using the given single or multiplex future futureSubscriber
     * A future getResult will be made available immediately the domain is processed by the futureSubscriber
     *
     * @return
     */
    @Override
    public Future<R> submitItem(T value) {
        return submitItemWithTTL(defaultTTLSeconds, value);
    }

    /**
     * Each putItem is processed as a Future<R> using the given a single flow or multiplexed flow future futureSubscriber
     * A future getResult will be made available immediately the domain is processed by the futureSubscriber
     *
     * @return
     */
    @Override
    public Future<R> submitItemWithTTL(long ttlSeconds, T value) {
        if (futureSubscriber != null)
            return putAndReturnAsCompletableFuture(ttlSeconds, value);
        throw new IllegalStateException(
                "Cannot submit item without future subscribers being registered");
    }


    public FunctionalStreamItemSubscriber<R,T> publish() {
        FunctionalStreamItemSubscriber functionalStreamItemSubscriber= new FunctionalStreamItemSubscriber<>(this);
        return functionalStreamItemSubscriber;
    }

    @Override
    public FlowContents contents() {
        return flowContents;
    }

    @Override
    public void close(boolean waitForEnd) {
        this.isEnd = true;
        try {
            if (waitForEnd)
                while (!this.hasEnded()) {
                    Thread.sleep(100);
                }
        } catch (InterruptedException e) {
        }

        synchronized (flowControlLock) {
            flowCount--;
            flowMap.remove(this.id);
            if (flowCount == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.reset();
        }
        logger.info("Flow " + name + " id " + id + " has stopped");
    }

    private void reset() {
        this.subscribers.forEach(this::unsubscribe);
        this.initialise(this.name, this.indexFieldName, this.ttl());
    }

    public Flowable<T, R> countDownLatch(int count_down_latch) {
        this.count_down_latch = count_down_latch;
        return this;
    }

    private Future<R> putAndReturnAsCompletableFuture(long ttlSeconds, T value) {
        long itemId = putItemWithTTL(ttlSeconds, value)[0];
        CompletableFuture<R> completableFuture = new CompletableFuture<>();
        return futureSubscriber.register(itemId, completableFuture);
    }

    private boolean activeSubscribers() {
        return hasActiveSubscribers();
    }

    public static int numActiveFlows() {
        synchronized (flowControlLock) {
            return flowCount;
        }
    }

    public boolean housekeep() {
        if (this.ttl() > 0)
            if (flowService().itemStoreContents.removeIf(item -> !item.alive())) {
                logger.log(Level.INFO, "House kept flow : " + this.name());
                return true;
            }
        return false;
    }


    public boolean hasEnded() {
        return this.isEnd && this.buffer_size() == 0 && !activeSubscribers();
    }

    public String indexFieldName() {
        return this.indexFieldName;
    }

    public Map<String, FlowSubscriptionTask> getSubscriberSubscriptions() {
        return this.subscriberSubscriptions;
    }

    protected void slowdown() {
        slowDownNanos = slowDownNanos + LOCK_PARK_NANOS;
    }

    protected void speedup() {
        if (slowDownNanos > 0)
            slowDownNanos = 0;
    }

    protected long slowDownNanos() {
        return slowDownNanos;
    }

    public void acceptAll() {
        int ticks = count_down_latch;
        while (!hasEnded() && ticks >= 0) {
            if (ticks == 0 || flush || item_queue.size() == queue_max_size || this.isEnd) {
                this.item_queue.drainTo(this.flowContents.itemStoreContents);
            }
            ticks--;
            LockSupport.parkNanos(100000);
        }
    }

    private void registerFutureSubscriber(FutureSubscriber<R, T> subscriber) {
        if (futureSubscriber == null) {
            subscribe(subscriber);
            this.futureSubscriber = subscriber;
        } else
            throw new IllegalStateException("Only one futureSubscriber may be added");

    }


    private boolean addToStreamWithBlock(Item<T, R> item, boolean immediately) {
        try {
            if (!this.item_queue.offer(item)) {
                this.flush = immediately;
                this.item_queue.put(item);
            }
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    private int buffer_size() {
        return this.item_queue.size();
    }

    public int size() {
        return this.item_queue.size() + this.contents().allValues().length;
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public FlowContents<T, R> flowService() {
        return flowContents;
    }

    @Override
    public long ttl() {
        return this.defaultTTLSeconds;
    }

    public boolean isAtEnd() {
        return isEnd && this.item_queue.size() == 0;
    }


    public Future<R> subscribe(SubscriberInterface<R, T> subscriber) {
        subscriber.initialise();
        synchronized (lockSubscriberslist) {
            this.subscribers.add(subscriber);
            this.lastSeenItemMap.put(subscriber.getId(), EmptyItem.EMPTY_ITEM);
            logger.info("Added subscriber " + subscriber.getId() + " flow " + name());
        }
        return subscriber.getResult();
    }

    public void unsubscribe(SubscriberInterface<R, T> subscriber) {
        synchronized (lockSubscriberslist) {
            if (subscribers.contains(subscriber)) {
                subscriber.finalise();
                subscriber.stop();
                subscriber.setResult(subscriber.getNext());
                this.subscribers.remove(subscriber);
                this.lastSeenItemMap.remove(subscriber.getId());
                logger.info("Removed subscriber " + subscriber.getId() + " flow " + name());
            }
        }
    }

    public boolean hasActiveSubscribers() {
        synchronized (lockSubscriberslist) {
            return subscribers.stream().anyMatch(s -> !s.isDone());
        }
    }


    public synchronized FlowSubscriptionTask newSubscriptionTask() {
        return new FlowSubscriptionTask(this);
    }

    public final class FlowSubscriptionTask implements Callable<Boolean> {

        Logger logger = Logger.getLogger(FlowSubscriptionTask.class.getCanonicalName());
        private Flow<T, R> itemStream;

        public FlowSubscriptionTask(Flow<T, R> itemStream) {
            this.itemStream = itemStream;
        }

        @Override
        public Boolean call() {
            {
                List<Callable<Boolean>> callableList;
                synchronized (lockSubscriberslist) {
                    callableList = subscribers.stream().map(subscriber -> (Callable<Boolean>) () -> {
                        try {
                            Item lastSeenItem = lastSeenItemMap.get(subscriber.getId());
                            if (!subscriber.isDone() && !itemStream.isAtEnd()) {
                                return !lastSeenItemMap.put(subscriber.getId(), processResults(subscriber, lastSeenItem)).equals(lastSeenItem);

                            } else {
                                processFinalResults(subscriber, lastSeenItem);
                                unsubscribe(subscriber);
                                logger.log(Level.FINE,
                                        "Subscriber " + subscriber.getId() + " stopped for stream : " + itemStream.name());
                                return true;
                            }
                        } catch (Exception e) {
                            return false;
                        }
                    }).collect(Collectors.toList());
                }

                if (callableList.size() > 0)
                    try {
                        Collections.shuffle(callableList);
                        Optional<Boolean> atLeastOnehasExecuted =
                                InternalExecutors.subscribersThreadPoolInstance().invokeAll(callableList).stream().map(f -> {
                                    try {
                                        //block for all subscriber tasks to finish
                                        return f.get();
                                    } catch (InterruptedException | ExecutionException e) {
                                        logger.log(Level.WARNING, "subcriber execution error", e);
                                    }
                                    return false;
                                }).filter(b -> b.equals(true)).findFirst();

                        return atLeastOnehasExecuted.orElse(false);
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "Subscriber task was interrupted will try again");
                    }
            }
            return false;
        }

        private void processFinalResults(SubscriberInterface<R, T> subscriber, Item lastSeenItem) {
            Item lastItemInStream = itemStream.contents().last();
            while (lastSeenItem==null | (!lastSeenItem.equals(lastItemInStream) && !lastItemInStream.equals(EmptyItem.EMPTY_ITEM))) {
                NavigableSet<Item<T, R>> streamContents = streamContentsSnapShot(subscriber, lastSeenItem);
                while (streamContents.size() > 0) {
                    subscriber.emit(streamContents);
                    lastSeenItem = streamContents.last();
                    streamContents = streamContentsSnapShot(subscriber, lastSeenItem);
                }
                lastItemInStream = itemStream.contents().last();
            }
            logger.info("Final results processed " + (lastSeenItem != null ? lastSeenItem.value() : "empty") + lastItemInStream.value());
        }

        private Item processResults(SubscriberInterface<R, T> subscriber, Item lastSeenItem) {
            NavigableSet<Item<T, R>> streamContents = streamContentsSnapShot(subscriber, lastSeenItem);
            if (streamContents.size() > 0) {
                subscriber.emit(streamContents);
                if (streamContents.size() > 0)
                    return streamContents.last();

            }
            return lastSeenItem;
        }

        private NavigableSet<Item<T, R>> streamContentsSnapShot(SubscriberInterface<R, T> subscriber, Item lastSeenItem) {
            if (subscriber.delayMS() > 0)
                LockSupport.parkUntil(System.currentTimeMillis() + subscriber.delayMS());
            NavigableSet<Item<T, R>> streamContents = Collections
                    .unmodifiableNavigableSet(this.itemStream.contents().allAfter(lastSeenItem));
            return streamContents;
        }

        public Optional<SubscriberInterface<R, T>> getSubscriber(String subscriberId) {
            return subscribers.stream().filter(s -> s.getId().equals(subscriberId)).findFirst();
        }

    }

}
