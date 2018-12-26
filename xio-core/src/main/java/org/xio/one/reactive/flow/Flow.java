/*
 * stream.java
 * Created on 14 October 2006, 10:20
 * Copyright Xio
 */
package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.domain.flow.*;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.ItemIdSequence;
import org.xio.one.reactive.flow.service.FlowContents;
import org.xio.one.reactive.flow.service.FlowDaemonService;
import org.xio.one.reactive.flow.subscriber.CompletableSubscriber;
import org.xio.one.reactive.flow.subscriber.FutureSubscriber;
import org.xio.one.reactive.flow.subscriber.Subscriber;
import org.xio.one.reactive.flow.subscriber.internal.SubscriberInterface;
import org.xio.one.reactive.flow.subscriber.internal.Subscription;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * An Flow - a anItemFlow stream of items
 *
 * <p>Flow is implemented with a Command Query Responsibility Segregation external objects,
 * json etc can be put into the anItemFlow and are then asynchronously loaded in memory to a
 * contents store that is used to provide a sequenced view of the flowing items to downstream
 * futureSubscriber
 */
public final class Flow<T, R>
    implements Flowable<T, R>, ItemFlow<T, R>, FutureItemResultFlowable<T, R>,
    CompletableItemFlowable<T, R> {

  public static final int LOCK_PARK_NANOS = 100000;
  // streamContents variables
  private FlowDaemonService<T, R> contentsControl;

  // constants
  private int count_down_latch = 10;
  private final int queue_max_size = 16384;

  // input parameters
  private String name;
  private String indexFieldName;

  public static final long DEFAULT_TIME_TO_LIVE_SECONDS = 10;
  private long defaultTTLSeconds = DEFAULT_TIME_TO_LIVE_SECONDS;


  private Map<String, Future> streamSubscriberFutureResultMap = new ConcurrentHashMap<>();
  private Map<String, Subscription<R, T>> subscriberSubscriptions = new ConcurrentHashMap<>();

  // Queue control
  private Item[] itemqueue_out;
  private BlockingQueue<Item<T, R>> item_queue;
  private volatile boolean isEnd = false;
  private volatile boolean flush = false;
  private ItemIdSequence itemIDSequence;
  private long slowDownNanos = 0;
  private boolean flushImmediately;
  private ExecutorService executorService = InternalExecutors.subscriberCachedThreadPoolInstance();

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

  private Flow(String name, String indexFieldName, long ttlSeconds) {
    this.item_queue = new ArrayBlockingQueue<>(queue_max_size, true);
    this.itemqueue_out = new Item[this.queue_max_size];
    this.name = name;
    this.indexFieldName = indexFieldName;
    if (ttlSeconds >= 0)
      this.defaultTTLSeconds = ttlSeconds;
    this.contentsControl = new FlowDaemonService<>(this);
    this.itemIDSequence = new ItemIdSequence();
    this.flushImmediately = false;

  }

  @Override
  public void addSubscriber(Subscriber<R, T> subscriber) {
    addAppropriateSubscriber(subscriber);
  }

  @Override
  public void removeSubscriber(String id) {

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
    Subscription<R, T> subscription = new Subscription<>(this, subscriber);
    streamSubscriberFutureResultMap.put(subscriber.getId(), subscription.subscribe());
    subscriberSubscriptions.put(subscriber.getId(), subscription);
  }

  private FutureSubscriber<R, T> futureSubscriber = null;

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
   * Indicate that core should be create using the given executor service
   * Use when low latency < 2ms is required for core
   *
   * @return
   */
  public Flowable<T, R> executorService(ExecutorService executorService) {
    this.executorService = executorService;
    return this;
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


  public ExecutorService executorService() {
    return executorService;
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
    Item<T, R> item =
        new Item<>(value, itemIDSequence.getNext(), ttlSeconds, completionHandler);
    addToStreamWithBlock(item, flushImmediately);
  }

  /**
   * Each putItem is processed as a Future<R> using the given single or multiplex future futureSubscriber
   * A future result will be made available immediately the domain is processed by the futureSubscriber
   *
   * @return
   */
  @Override
  public Future<R> submitItem(T value) {
    return submitItemWithTTL(defaultTTLSeconds, value);
  }

  /**
   * Each putItem is processed as a Future<R> using the given a single flow or multiplexed flow future futureSubscriber
   * A future result will be made available immediately the domain is processed by the futureSubscriber
   *
   * @return
   */
  @Override
  public Future<R> submitItemWithTTL(long ttlSeconds, T value) {
    if (futureSubscriber != null)
      return putAndReturnAsCompletableFuture(ttlSeconds, value);
    throw new IllegalStateException(
        "Cannot submit item without future subscriber being registered");
  }

  @Override
  public FlowContents contents() {
    return contentsControl.query();
  }

  @Override
  public void end(boolean waitForEnd) {
    this.isEnd = true;
    try {
      if (waitForEnd)
        while (!this.hasEnded() || activeSubscriptions()) {
          Thread.sleep(100);
        }

    } catch (InterruptedException e) {
    }
  }

  @Override
  public Flowable<T, R> countDownLatch(int count_down_latch) {
    this.count_down_latch = count_down_latch;
    return this;
  }

  private Future<R> putAndReturnAsCompletableFuture(long ttlSeconds, T value) {
    long itemId = putItemWithTTL(ttlSeconds, value)[0];
    CompletableFuture<R> completableFuture = new CompletableFuture<>();
    return futureSubscriber.register(itemId, completableFuture);
  }

  private boolean activeSubscriptions() {
    Optional<Subscription<R, T>> any =
        this.getSubscriberSubscriptions().values().stream().filter(s -> !s.getSubscriber().isDone())
            .findAny();
    return any.isPresent();
  }

  public boolean hasEnded() {
    return this.isEnd && this.buffer_size() == 0;
  }

  public String indexFieldName() {
    return this.indexFieldName;
  }

  public Map<String, Subscription<R, T>> getSubscriberSubscriptions() {
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
    while (!hasEnded()) {
      if (ticks == 0 || flush || item_queue.size() == queue_max_size) {
        this.item_queue.drainTo(this.contentsControl.getItemRepositoryContents());
        ticks = count_down_latch;
      }
      ticks--;
      LockSupport.parkNanos(100000);
    }
  }

  private void registerFutureSubscriber(FutureSubscriber<R, T> subscriber) {
    if (futureSubscriber == null) {
      Subscription<R, T> subscription = new Subscription<>(this, subscriber);
      Collections.synchronizedMap(subscriberSubscriptions).put(subscriber.getId(), subscription);
      subscription.subscribe();
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
    return this.item_queue.size() + this.contents().getItemStoreContents().size();
  }

  public boolean isEmpty() {
    return this.item_queue.isEmpty() && this.contents().getItemStoreContents().isEmpty();
  }

}
