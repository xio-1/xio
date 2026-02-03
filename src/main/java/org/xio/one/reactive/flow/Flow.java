/*
 * stream.java
 * Created on 14 October 2006, 10:20
 * Copyright Xio
 */
package org.xio.one.reactive.flow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.xio.one.reactive.flow.domain.FlowException;
import org.xio.one.reactive.flow.domain.flow.*;
import org.xio.one.reactive.flow.domain.item.CompletableItem;
import org.xio.one.reactive.flow.domain.item.EmptyItem;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.ItemIdSequence;
import org.xio.one.reactive.flow.domain.item.ItemSequenceComparator;
import org.xio.one.reactive.flow.domain.item.VoidItem;
import org.xio.one.reactive.flow.domain.item.logging.ItemDeserializer;
import org.xio.one.reactive.flow.domain.item.logging.ItemLogger;
import org.xio.one.reactive.flow.internal.Housekeeper;
import org.xio.one.reactive.flow.internal.RecoverySnapshot;
import org.xio.one.reactive.flow.internal.SimpleHousekeeper;
import org.xio.one.reactive.flow.subscribers.FunctionalSubscriber;
import org.xio.one.reactive.flow.subscribers.FutureSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.AbstractSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.CompletableSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;
import org.xio.one.reactive.flow.util.InternalExecutors;

/**
 * Flow
 * <p>
 * a anItemFlow stream of items
 * <p>
 * Flow is implemented with a Command Query Responsibility Segregation external objects, json etc
 * can be put into the anItemFlow and are then asynchronously loaded in memory to a getSink store
 * that is used to provide a sequenced view of the flowing items to downstream futureSubscriber
 *
 * @Author Richard Durley
 * @OringinalWork XIO
 * @Copyright Richard Durley / XIO.ONE
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 */
public class Flow<T, R> implements Flowable<T, R>, ItemFlowable<T, R>, FutureItemFlowable<T, R>,
        CompletableItemFlowable<T, R> {

    private static final int LOCK_PARK_NANOS = 100000;
    private static final long DEFAULT_TIME_TO_LIVE_SECONDS = 60;
    private static final Object flowControlLock = new Object();
    private static final Logger logger = Logger.getLogger(Flow.class.getName());
    // allItems flows
    private static final Map<String, Flow> flowMap = new ConcurrentHashMap<>();
    private static final AtomicInteger flowCount = new AtomicInteger();

    private final int queue_max_size = 16384;
    private final Object lockSubscriberslist = new Object();
    private final Object lockFlowContents = new Object();
    private final String id;
    private final Map<String, FlowSubscriptionTask> subscriberSubscriptions = new ConcurrentHashMap<>();
    private final int DEFAULT_MAX_TAKE_SIZE = Integer.MAX_VALUE;
    private Map<String, Item<T>> lastSeenItemMap;
    // streamContentsSnapShot variables
    private ItemSink<T> flowContents;
    // constants
    private int count_down_latch = 10;
    private final CountDownLatch countDownLatch = new CountDownLatch(count_down_latch);
    // input parameters
    private String name;
    private String indexFieldName;
    private long maxTTLSeconds = DEFAULT_TIME_TO_LIVE_SECONDS;
    // Queue control
    private BlockingQueue<Item<T>> item_queue;
    private volatile boolean isEnd = false;
    private volatile boolean flush = false;
    private ItemIdSequence itemIDSequence;
    private long slowDownNanos = 0;
    private boolean flushImmediately;
    //subscription control
    private List<Subscriber<R, T>> subscribers;
    private List<FutureSubscriber<R, T>> futureSubscribers;
    private ItemLogger<T> itemLogger;
    private boolean loggingEnabled = false;
    private Housekeeper housekeeper;

    private Flow(String name, String indexFieldName, long ttlSeconds, ItemLogger itemLogger) {
        if (itemLogger != null) {
            this.loggingEnabled = true;
        }
        this.housekeeper = new SimpleHousekeeper();
        this.id = UUID.randomUUID().toString();
        initialise(name, indexFieldName, ttlSeconds);
        synchronized (flowControlLock) {
            flowMap.put(id, this);
            if (XIOService.isRunning()) {
                logger.info("XIO Service is running ");
            } else {
                XIOService.start();
            }
            logger.info("Flow " + name + " id " + id + " has started");
        }
    }

    //bad use of erasure need to find a better way
    public static <T, R> ItemFlowable<T, R> anItemFlow() {
        return new Flow<>(UUID.randomUUID().toString(), null, DEFAULT_TIME_TO_LIVE_SECONDS, null);
    }

    public static <T, R> ItemFlowable<T, R> anItemFlow(String name) {
        return new Flow<>(name, null, DEFAULT_TIME_TO_LIVE_SECONDS, null);
    }

    public static <T, R> ItemFlowable<T, R> anItemFlow(String name, ItemLogger<T> itemLogger) {
        return new Flow<>(name, null, DEFAULT_TIME_TO_LIVE_SECONDS, itemLogger);
    }

    public static <T, R> ItemFlowable<T, R> anItemFlow(String name, long maxTTLSeconds) {
        return new Flow<>(name, null, maxTTLSeconds, null);
    }

    public static <T, R> ItemFlowable<T, R> anItemFlow(String name, long maxTTLSeconds,
                                                       ItemLogger<T> itemLogger) {
        return new Flow<>(name, null, maxTTLSeconds, itemLogger);
    }

    public static <T, R> ItemFlowable<T, R> anItemFlow(String name, String indexFieldName) {
        return new Flow<>(name, indexFieldName, DEFAULT_TIME_TO_LIVE_SECONDS, null);
    }

    public static <T, R> ItemFlowable<T, R> anItemFlow(String name, String indexFieldName,
                                                       long maxTTLSeconds) {
        return new Flow<>(name, indexFieldName, maxTTLSeconds, null);
    }

    public static <T, R> FutureItemFlowable<T, R> aFutureItemFlow() {
        Flow<T, R> resultFlowable =
                new Flow<>(UUID.randomUUID().toString(), null, DEFAULT_TIME_TO_LIVE_SECONDS, null);
        return resultFlowable;
    }

    public static <T, R> FutureItemFlowable<T, R> aFutureItemFlow(
            FutureSubscriber<R, T> futureSubscriber) {
        Flow<T, R> resultFlowable =
                new Flow<>(UUID.randomUUID().toString(), null, DEFAULT_TIME_TO_LIVE_SECONDS, null);
        resultFlowable.addAppropriateSubscriber(futureSubscriber);
        return resultFlowable;
    }

    public static <T, R> FutureItemFlowable<T, R> aFutureItemFlow(String name,
                                                                  FutureSubscriber<R, T> futureSubscriber) {
        Flow<T, R> resultFlowable = new Flow<>(name, null, DEFAULT_TIME_TO_LIVE_SECONDS, null);
        resultFlowable.addAppropriateSubscriber(futureSubscriber);
        return resultFlowable;
    }

    public static <T, R> FutureItemFlowable<T, R> aFutureItemFlow(String name, long maxTTLSeconds,
                                                                  FutureSubscriber<R, T> futureSubscriber) {
        Flow<T, R> resultFlowable = new Flow<>(name, null, maxTTLSeconds, null);
        resultFlowable.addAppropriateSubscriber(futureSubscriber);
        return resultFlowable;
    }

    public static <T, R> CompletableItemFlowable<T, R> aCompletableItemFlow(
            CompletableSubscriber<R, T> completableSubscriber) {
        Flow<T, R> resultFlowable =
                new Flow<>(UUID.randomUUID().toString(), null, DEFAULT_TIME_TO_LIVE_SECONDS, null);
        resultFlowable.addAppropriateSubscriber(completableSubscriber);
        return resultFlowable;
    }

    public static <T, R> CompletableItemFlowable<T, R> aCompletableItemFlow(String name,
                                                                            CompletableSubscriber<R, T> completableSubscriber) {
        Flow<T, R> resultFlowable = new Flow<>(name, null, DEFAULT_TIME_TO_LIVE_SECONDS, null);
        resultFlowable.addAppropriateSubscriber(completableSubscriber);
        return resultFlowable;
    }

    public static <T, R> CompletableItemFlowable<T, R> aCompletableItemFlow(String name,
                                                                            long maxTTLSeconds, CompletableSubscriber<R, T> completableSubscriber) {
        Flow<T, R> resultFlowable = new Flow<>(name, null, maxTTLSeconds, null);
        resultFlowable.addAppropriateSubscriber(completableSubscriber);
        return resultFlowable;
    }

    public static Collection<Flow> allFlows() {
        synchronized (flowControlLock) {
            return Collections.synchronizedMap(flowMap).values();
        }
    }

    public static Flow forID(String id) {
        synchronized (flowControlLock) {
            return Collections.synchronizedMap(flowMap).get(id);
        }
    }

    public static int numActiveFlows() {
        return flowCount.get();
    }

    private void initialise(String name, String indexFieldName, long maxTTLSeconds) {
        synchronized (flowControlLock) {
            this.item_queue = new ArrayBlockingQueue<>(queue_max_size, true);
            this.name = name;
            this.indexFieldName = indexFieldName;
            if (maxTTLSeconds >= 0) {
                this.maxTTLSeconds = maxTTLSeconds;
            }
            synchronized (lockFlowContents) {
                this.flowContents = new ItemSink<>(this);
            }
            this.itemIDSequence = new ItemIdSequence();
            this.flushImmediately = false;
            synchronized (lockSubscriberslist) {
                this.subscribers = Collections.synchronizedList(new ArrayList<>());

                this.futureSubscribers = Collections.synchronizedList(new ArrayList<>());
                this.lastSeenItemMap = Collections.synchronizedMap(new ConcurrentHashMap<>());
            }
        }
    }

    @Override
    public void resetLastSeenItem(String subscriberID, Item<T> lastSeenItem) {
        synchronized (lockSubscriberslist) {
            lastSeenItemMap.put(subscriberID, lastSeenItem);
        }
    }

    @Override
    public void recoverSnapshot(RecoverySnapshot<R, T> snapshot) {
        synchronized (flowControlLock) {
            synchronized (lockFlowContents) {
                this.lastSeenItemMap = snapshot.getLastSeenItemMap();
                this.itemIDSequence = new ItemIdSequence(snapshot.getItemID());
                this.flowContents = new ItemSink<>(this);
                this.flowContents.getItemStoreContents().addAll(snapshot.getContents());
                synchronized (lockSubscriberslist) {
                    //this.subscribers = snapshot.getSubscribers();
                    //this.futureSubscribers = snapshot.getFutureSubscribers();
                }
            }
        }
    }

    public void replayLog() {

    }

    @Override
    public Subscriber<R, T> addSubscriber(Subscriber<R, T> subscriber) {
        addAppropriateSubscriber(subscriber);
        return subscriber;
    }

    @Override
    public Subscriber<R, T> getSubscriber(String id) {
        synchronized (lockSubscriberslist) {
            return subscribers.stream().filter(p -> p.getId().equals(id)).findFirst().get();
        }
    }

    @Override
    public void removeSubscriber(Subscriber<R, T> subscriber) {
        unsubscribe(subscriber);
    }

    private void addAppropriateSubscriber(Subscriber<R, T> subscriber) {
        if (subscriber instanceof AbstractSubscriber) {
            registerSubscriber(subscriber);
        }
        if (subscriber instanceof FutureSubscriber) {
            registerFutureSubscriber((FutureSubscriber<R, T>) subscriber);
        }
        if (subscriber instanceof CompletableSubscriber) {
            registerSubscriber(subscriber);
        }
    }

    private void registerSubscriber(Subscriber<R, T> subscriber) {
        subscribe(subscriber);
    }

    /**
     * Indicate that each item place should be flushed immediately Use when low latency < 2ms is
     * required for core
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
    public String getUUID() {
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
     * Put a putItem value into the getSink
     */

    @Override
    public long putItem(T value) {
        return putItemWithTTL(maxTTLSeconds, value)[0];
    }

    /**
     * Puts list anItemFlow values into the getSink
     *
     * @param values
     * @return
     */

    @Override
    public long[] putItem(T... values) {
        return putItemWithTTL(maxTTLSeconds, values);
    }

    /**
     * Puts list anItemFlow values into the getSink with ttlSeconds
     *
     * @param values
     * @return
     */

    @Override
    public long[] putItemWithTTL(long ttlSeconds, T... values) {
        long[] ids = new long[values.length];
        if (ttlSeconds > this.maxTTLSeconds) {
            throw new FlowException(
                    "Time to live cannot exceed maximum for flow " + this.maxTTLSeconds);
        }
        if (!isEnd) {
            for (int i = 0; i < values.length; i++) {
                Item<T> item = new Item<>(values[i], itemIDSequence.getNext(), ttlSeconds);
                if (isLoggingEnabled() && this.itemLogger != null) {
                    this.itemLogger.logItem(item);
                }
                ids[i] = item.getItemId();
                if (slowDownNanos > 0) {
                    LockSupport.parkNanos(slowDownNanos);
                }
                addToStreamWithBlock(item, flushImmediately);
            }
        } else throw new FlowAlreadyClosedException("Flow " + this.name() + " already closed");
        return ids;
    }

    @Override
    public void addItemLogger(ItemLogger<T> logger) {
        this.itemLogger = logger;
        this.loggingEnabled = true;
    }

    @Override
    public void recoverItemsFromLog(String filename, ItemDeserializer<T> deserializer) {
        File logFile = new File(filename);

    }

    /**
     * Submit an item to be processed by the FutureSubscriber and completionHandler to the completion
     * handler
     *
     * @param value
     * @param completionHandler
     */
    @Override
    public void submitItem(T value, FlowItemCompletionHandler<R, T> completionHandler) {
        submitItemWithTTL(this.maxTTLSeconds, value, completionHandler);
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
        if (ttlSeconds > this.maxTTLSeconds) {
            throw new FlowException(
                    "Time to live cannot exceed maximum for flow " + this.maxTTLSeconds);
        }
        Item<T> item =
                new CompletableItem<>(value, itemIDSequence.getNext(), ttlSeconds, completionHandler);
        if (isLoggingEnabled()) {
            this.itemLogger.logItem(item);
        }
        addToStreamWithBlock(item, flushImmediately);
    }

    /**
     * Each putItem is processed as a Future<R> using the given single or multiplex future
     * futureSubscriber A future getFutureResult will be made available immediately the domain is
     * processed by the futureSubscriber
     *
     * @return
     */
    @Override
    public Promise<R> submitItem(T value) {
        return submitItemWithTTL(maxTTLSeconds, value);
    }

    /**
     * Each putItem is processed as a Future<R> using the given a single flow or multiplexed flow
     * future futureSubscriber A future getFutureResult will be made available immediately the domain
     * is processed by the futureSubscriber
     *
     * @return
     */
    @Override
    public Promise<R> submitItemWithTTL(long ttlSeconds, T value) {
        if (ttlSeconds > this.maxTTLSeconds) {
            throw new FlowException(
                    "Time to live cannot exceed maximum for flow " + this.maxTTLSeconds);
        }
        if (futureSubscribers.size() > 0) {
            return putAndReturnAsCompletableFuture(ttlSeconds, value);
        }
        throw new IllegalStateException(
                "Cannot submit item without future subscribers being registered");
    }

    public FunctionalSubscriber<R, T> publishTo(Class clazz) {
        return new FunctionalSubscriber<>(this, clazz);
    }

    @Override
    public ItemSink getSink() {
        return flowContents;
    }

    @Override
    public void close(boolean waitForEnd) {
        this.isEnd = true;
        try {
            if (waitForEnd) {
                while (!this.hasEnded()) {
                    Thread.sleep(100);
                }
            }
            if (loggingEnabled && itemLogger != null) {
                this.itemLogger.close(waitForEnd);
            }
        } catch (InterruptedException e) {
        }

        if (waitForEnd) {
            synchronized (lockSubscriberslist) {
                while (this.subscribers.iterator().hasNext())
                    this.unsubscribe(this.subscribers.iterator().next());
            }
        }

        synchronized (flowControlLock) {
            flowMap.remove(this.id);
            if (flowCount.decrementAndGet() == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                InternalExecutors.schedulerThreadPoolInstance().shutdown();
                InternalExecutors.daemonThreadPoolInstance().shutdown();
            }
        }
        logger.info("Flow " + name + " id " + id + " has stopped");
    }

    public void restoreAllSubscribers(Map<String, Map<String, Object>> subscriberContext, RestorableSubscriber<R, T> restorableSubscriber) {
        subscriberContext.forEach((k, v) -> this.addSubscriber(
                restorableSubscriber.restore(k, subscriberContext.get(k))));
    }

    public void reset() {
        synchronized (flowControlLock) {
            synchronized (lockSubscriberslist) {
                while (this.subscribers.iterator().hasNext())
                    this.unsubscribe(subscribers.iterator().next());
                this.initialise(this.name, this.indexFieldName, this.maxTTLSeconds);
            }
        }
    }

    public Flowable<T, R> countDownLatch(int count_down_latch) {
        this.count_down_latch = count_down_latch;
        return this;
    }

    public Object getLockFlowContents() {
        return lockFlowContents;
    }

    public Object getLockSubscriberslist() {
        return lockSubscriberslist;
    }

    private Promise<R> putAndReturnAsCompletableFuture(long ttlSeconds, T value) {
        long itemId = putItemWithTTL(ttlSeconds, value)[0];
        Promise<R> promise = new Promise<>();
        futureSubscribers.forEach(s -> {
            CompletableFuture<R> completableFuture = new CompletableFuture<>();
            promise.addPromise(s.getId(), completableFuture);
            s.registerCompletableFuture(itemId, completableFuture);
        });
        return promise;
    }

    private boolean activeSubscribers() {
        return hasActiveSubscribers();
    }

    public boolean housekeep() {
        return this.housekeeper.housekeep(this);
    }

    public void addCustomHousekeeper(Housekeeper housekeeper) {
        this.housekeeper = housekeeper;
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
        if (slowDownNanos > 0) {
            slowDownNanos = 0;
        }
    }

    protected long slowDownNanos() {
        return slowDownNanos;
    }

 /* public void acceptAll() {
    //int ticks = count_down_latch;
    while (!hasEnded()) {
      try {
        if (flush || item_queue.size() == queue_max_size || this.isEnd) {
          drainToSink();
        } else {
          countDownLatch.await(100000, TimeUnit.NANOSECONDS);
          drainToSink();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        countDownLatch = new CountDownLatch(count_down_latch);
        LockSupport.parkNanos(100000);
      }
    }
  }

  private void drainToSink() {
    synchronized (lockFlowContents) {
      if (this.item_queue.drainTo(this.flowContents.itemStoreContents) > 0)
        XIOService.getXioBoss().getFlowSubscriptionMonitor().unpark();
    }
  }*/

    public boolean acceptAll() {
        int ticks = count_down_latch;
        boolean processed = false;
        while (!hasEnded() && ticks >= 0) {
            if (ticks == 0 || flush || item_queue.size() == queue_max_size || this.isEnd) {
                synchronized (lockFlowContents) {
                    if (this.item_queue.drainTo(this.flowContents.itemStoreContents) > 0) {
                        XIOService.getXioBoss().getFlowSubscriptionMonitor().unpark();
                        processed = true;
                    } else
                        LockSupport.parkNanos(100000);
                }
            }
            ticks--;

        }
        return processed;
    }


    private void registerFutureSubscriber(FutureSubscriber<R, T> subscriber) {
        if (!futureSubscribers.contains(subscriber)) {
            subscribe(subscriber);
            futureSubscribers.add(subscriber);
        }
    }


    private boolean addToStreamWithBlock(Item<T> item, boolean immediately) {
        try {
            if (!this.item_queue.offer(item)) {
                this.flush = immediately;
                this.item_queue.put(item);
            }
            //countDownLatch.countDown();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    public int buffer_size() {
        return this.item_queue.size();
    }

    public int size() {
        return this.item_queue.size() + this.getSink().allValues().length;
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    private ItemSink<T> flowContents() {
        return flowContents;
    }

    @Override
    public long maxTTLSeconds() {
        return this.maxTTLSeconds;
    }

    public boolean isAtEnd() {
        return isEnd && this.item_queue.size() == 0;
    }

    public Item[] takeSinkSnapshot() {
        long start = System.currentTimeMillis();
        while (true) {
            synchronized (lockFlowContents) {
                if (this.size() == getSink().size()) {
                    return getSink().allItems();
                }
            }
            if (start + 10000 <= System.currentTimeMillis()) {
                throw new FlowException("Sink is Locked Snapshot Failed");
            } else {
                LockSupport.parkNanos(LOCK_PARK_NANOS);
            }
        }
    }

    //Items and last seen item are final and thread safe
    public RecoverySnapshot<R, T> takeRecoverySnapshot(boolean full) {
        long start = System.currentTimeMillis();
        NavigableSet<Item<T>> contents;
        ItemSequenceComparator<T> itemSequenceComparator = new ItemSequenceComparator<>();
        Map<String, Map<String, Object>> subscriberContext = new HashMap<>();
        Map<String, Item> lastSeenItemMap = new ConcurrentHashMap<>();
        while (true) {
            synchronized (lockFlowContents) {
                this.item_queue.drainTo(this.flowContents.itemStoreContents);
                if (this.size() == getSink().size()) {
                    synchronized (lockSubscriberslist) {
                        Optional<Item<T>> minItem = this.lastSeenItemMap.values().stream()
                                .min(itemSequenceComparator);
                        if (!full && minItem.isPresent()) {
                            contents = getSink().allAfter(minItem.get(), this.size());
                        } else {
                            contents = getSink().allAfter(EmptyItem.EMPTY_ITEM, this.size());
                        }

                        lastSeenItemMap.putAll(this.lastSeenItemMap);
                        subscribers.forEach(s -> subscriberContext.put(s.getId(), s.getContext()));
                    }
                    return new RecoverySnapshot(itemIDSequence.getCurrent(), contents, lastSeenItemMap,
                            subscriberContext);
                }
            }
            if (start + 10000 <= System.currentTimeMillis()) {
                throw new FlowException("Sink is Locked Snapshot Failed");
            } else {
                LockSupport.parkNanos(LOCK_PARK_NANOS);
            }
        }
    }

    private void cyclelogs() {

    }


    public Future<R> subscribe(Subscriber<R, T> subscriber) {
        synchronized (lockSubscriberslist) {
            subscriber.initialise();
            this.subscribers.add(subscriber);
            this.lastSeenItemMap.put(subscriber.getId(), VoidItem.VOID_ITEM);
            logger.info("Added subscriber " + subscriber.getId() + " flow " + name());
        }
        return subscriber.getFutureResult();
    }


    public void unsubscribe(Subscriber<R, T> subscriber) {
        synchronized (lockSubscriberslist) {
            if (subscribers.contains(subscriber)) {
                subscriber.exitAndReturn(subscriber.finalise());
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

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }


    public final class FlowSubscriptionTask implements Callable<Boolean> {

        private final Flow<T, R> itemStream;
        Logger logger = Logger.getLogger(FlowSubscriptionTask.class.getCanonicalName());

        public FlowSubscriptionTask(Flow<T, R> itemStream) {
            this.itemStream = itemStream;
        }

        private CountDownLatch latch;

        @Override
        public Boolean call() {
            {
                List<Callable<Void>> callableList;
                synchronized (lockSubscriberslist) {
                    latch = new CountDownLatch(subscribers.size());
                    callableList = subscribers.stream().map(subscriber -> (Callable<Void>) () -> {
                        try {
                            Item lastSeenItem = lastSeenItemMap.get(subscriber.getId());
                            //if (VoidItem.VOID_ITEM.equals(lastSeenItem))
                            //  subscriber.initialise();
                            if (!subscriber.isDone() && !itemStream.isAtEnd()) {
                                Item last = processResults(subscriber, lastSeenItem);
                                lastSeenItemMap.replace(subscriber.getId(), lastSeenItem, last);
                            } else {
                                processFinalResults(subscriber, lastSeenItem);
                                unsubscribe(subscriber);
                                logger.log(Level.INFO,
                                        "Subscriber " + subscriber.getId() + " stopped for stream : "
                                                + itemStream.name());
                            }
                        } catch (FlowException e) {
                            logger.log(Level.WARNING, "subscriber execution error unsubscribing" + subscriber.getId() + " ", e);
                            unsubscribe(subscriber);
                        } finally {
                            latch.countDown();
                        }
                        return null;
                    }).collect(Collectors.toList());

                }

                if (!callableList.isEmpty()) {
                    try {
                        Collections.shuffle(callableList);

                                InternalExecutors.microFlowInputTaskThreadPoolInstance()
                                        .invokeAll(callableList)
                                        .stream().map(f -> {
                                            try {
                                               latch.await();
                                            } catch (InterruptedException e) {
                                                logger.log(Level.SEVERE, "cannot continue a subscribers execution was interrupted error " + f.state().name(), e);
                                                System.exit(-1);
                                            }
                                            return true;
                                        });

                        return true;
                    } catch (InterruptedException e) {
                        logger.log(Level.SEVERE, "cannot continue a subscribers execution was interrupted",e);
                        System.exit(-1);
                    }
                }
            }
            return false;
        }

        private void processFinalResults(Subscriber<R, T> subscriber, Item lastSeenItem) {
            Item lastItemInStream = itemStream.getSink().lastItem();
            while (lastSeenItem == null | (!lastItemInStream.equals(lastSeenItem)
                    && !lastItemInStream.equals(EmptyItem.EMPTY_ITEM))) {
                NavigableSet<Item<T>> streamContents = streamContentsSnapShot(subscriber, lastSeenItem);
                while (streamContents.size() > 0) {
                    subscriber.emit(streamContents);
                    lastSeenItem = streamContents.last();
                    streamContents = streamContentsSnapShot(subscriber, lastSeenItem);
                }
                lastItemInStream = itemStream.getSink().lastItem();
            }
            logger.info("Subscriber " + subscriber.getId() + " finished subscribing to flow "
                    + this.itemStream.name());

        }

        private Item processResults(Subscriber<R, T> subscriber, Item lastSeenItem) {
            NavigableSet<Item<T>> streamContents = streamContentsSnapShot(subscriber, lastSeenItem);
            if (streamContents.size() > 0) {
                subscriber.emit(streamContents);
                if (streamContents.size() > 0) {
                    return streamContents.last();
                }

            }
            return lastSeenItem;
        }

        private NavigableSet<Item<T>> streamContentsSnapShot(Subscriber<R, T> subscriber,
                                                             Item lastSeenItem) {
            if (subscriber.delayMS() > 0) {
                LockSupport.parkUntil(System.currentTimeMillis() + subscriber.delayMS());
            }
            NavigableSet<Item<T>> streamContents = Collections.unmodifiableNavigableSet(
                    this.itemStream.getSink().allAfter(lastSeenItem, DEFAULT_MAX_TAKE_SIZE));
            return streamContents;
        }

        public Optional<Subscriber<R, T>> getSubscriber(String subscriberId) {
            synchronized (lockSubscriberslist) {
                return subscribers.stream().filter(s -> s.getId().equals(subscriberId)).findFirst();
            }
        }


    }

}
