/*
 * stream.java
 * Created on 14 October 2006, 10:20
 * Copyright Xio
 */
package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.domain.CompletableFlowItem;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.domain.ItemIdSequence;
import org.xio.one.reactive.flow.service.FlowContents;
import org.xio.one.reactive.flow.service.FlowService;
import org.xio.one.reactive.flow.subscriber.FutureSubscriber;
import org.xio.one.reactive.flow.subscriber.Subscriber;
import org.xio.one.reactive.flow.subscriber.internal.SubscriberInterface;
import org.xio.one.reactive.flow.subscriber.internal.Subscription;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.nio.channels.CompletionHandler;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * An Flow - a aFlowable stream of items
 *
 * <p>Flow is implemented with a Command Query Responsibility Segregation external objects,
 * json etc can be put into the aFlowable and are then asynchronously loaded in memory to a
 * contents store that is used to provide a sequenced view of the flowing items to downstream
 * futureSubscriber
 */
public final class Flow<T, R> implements Flowable<T, R> {

  public static final int LOCK_PARK_NANOS = 100000;
  // streamContents variables
  private FlowService<T, R> contentsControl;

  // constants
  private int count_down_latch = 10;
  private final int queue_max_size = 100000;

  // input parameters
  private String name;
  private String indexFieldName;

  public static final long DEFAULT_TIME_TO_LIVE_SECONDS = 10;
  private long defaultTTLSeconds = DEFAULT_TIME_TO_LIVE_SECONDS;


  private Map<String, Future> streamSubscriberFutureResultMap = new ConcurrentHashMap<>();
  private Map<String, Subscription<R, T>> subscriberSubscriptions = new ConcurrentHashMap<>();

  // Queue control
  private FlowItem[] itemqueue_out;
  private BlockingQueue<FlowItem<T>> item_queue;
  private int last_queue_size = 0;
  private volatile boolean isEnd = false;
  private volatile boolean flush = false;
  private ItemIdSequence itemIDSequence;

  // locks
  private final Object lock = new Object();
  private long slowDownNanos = 0;
  private boolean flushImmediately = false;
  private ExecutorService executorService = InternalExecutors.subscriberCachedThreadPoolInstance();

  //bad use of erasure need too find a better way
  public static <T, R> Flowable<T, R> aFlowable() {
    return new Flow<>(UUID.randomUUID().toString(), null, DEFAULT_TIME_TO_LIVE_SECONDS);
  }

  public static <T, R> Flowable<T, R> aFlowable(String name) {
    return new Flow<>(name, null, DEFAULT_TIME_TO_LIVE_SECONDS);
  }

  public static <T, R> Flowable<T, R> aFlowable(String name, long ttlSeconds) {
    return new Flow<>(name, null, ttlSeconds);
  }

  public static <T, R> Flowable<T, R> aFlowable(String name, String indexFieldName) {
    return new Flow<>(name, indexFieldName, DEFAULT_TIME_TO_LIVE_SECONDS);
  }

  public static <T, R> Flowable<T, R> aFlowable(String name, String indexFieldName,
      long ttlSeconds) {
    return new Flow<>(name, indexFieldName, ttlSeconds);
  }

  private Flow(String name, String indexFieldName, long ttlSeconds) {
    this.item_queue = new ArrayBlockingQueue<>(queue_max_size, true);
    this.itemqueue_out = new FlowItem[this.queue_max_size];
    this.name = name;
    this.indexFieldName = indexFieldName;
    if (ttlSeconds >= 0)
      this.defaultTTLSeconds = ttlSeconds;
    this.contentsControl = new FlowService<>(this);
    this.itemIDSequence = new ItemIdSequence();
    this.flushImmediately = false;

  }

  public Flowable<T, R> addSubscriber(SubscriberInterface<R, T> subscriber) {
    if (subscriber instanceof Subscriber)
      registerSubscriber((Subscriber<R, T>) subscriber);
    if (subscriber instanceof FutureSubscriber)
      registerFutureSubscriber((FutureSubscriber<R, T>) subscriber);
    return this;
  }

  private void registerSubscriber(Subscriber<R, T> subscriber) {
    Subscription<R, T> subscription = new Subscription<>(this, subscriber);
    streamSubscriberFutureResultMap.put(subscriber.getId(), subscription.subscribe());
    subscriberSubscriptions.put(subscriber.getId(), subscription);
  }

  private FutureSubscriber<R, T> futureSubscriber = null;

  private void registerFutureSubscriber(FutureSubscriber<R, T> subscriber) {
    if (futureSubscriber==null) {
      Subscription<R, T> subscription = new Subscription<>(this, subscriber);
      Collections.synchronizedMap(subscriberSubscriptions).put(subscriber.getId(), subscription);
      subscription.subscribe();
      this.futureSubscriber = subscriber;
    } else
      throw new IllegalStateException("Only one futureSubscriber may be added");

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
   * Puts list aFlowable values into the contents
   *
   * @param values
   * @return
   */

  @Override
  public long[] putItem(T... values) {
    return putItemWithTTL(defaultTTLSeconds, values);
  }

  /**
   * Puts list aFlowable values into the contents with ttlSeconds
   *
   * @param values
   * @return
   */

  @Override
  public long[] putItemWithTTL(long ttlSeconds, T... values) {
    long[] ids = new long[values.length];
    if (!isEnd) {
      for (int i = 0; i < values.length; i++) {
        FlowItem<T> item = new FlowItem<>(values[i], itemIDSequence.getNext(), ttlSeconds);
        ids[i] = item.itemId();
        addToStreamWithNoBlock(item, flushImmediately);
        if (slowDownNanos > 0)
          LockSupport.parkNanos(slowDownNanos);
      }
    }
    return ids;
  }


  /**
   * Put's an item with the given futureSubscriber and completion handler
   *
   * @param value
   * @param subscriber
   * @param completionHandler
   */
  public void putItem(T value, FutureSubscriber<R, T> subscriber,
      CompletionHandler<R, T> completionHandler) {
    putItemWithTTL(this.defaultTTLSeconds, value, subscriber, completionHandler);
  }

  /**
   * Put's an item with the given futureSubscriber and completion handler
   *
   * @param value
   * @param subscriber
   * @param completionHandler
   */
  public void putItemWithTTL(long ttlSeconds, T value, FutureSubscriber<R, T> subscriber,
      CompletionHandler<R, T> completionHandler) {
    FlowItem<T> item =
        new CompletableFlowItem<>(value, itemIDSequence.getNext(), ttlSeconds, completionHandler);
    addToStreamWithNoBlock(item, flushImmediately);
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


  private Future<R> putAndReturnAsCompletableFuture(long ttlSeconds, T value) {
    long itemId = putItemWithTTL(ttlSeconds, value)[0];
    CompletableFuture<R> completableFuture = new CompletableFuture<>();
    return futureSubscriber.register(itemId, completableFuture);
  }



  /**
   * Return a queryable facade for Flow's contents
   *
   * @return
   */
  @Override
  public FlowContents contents() {
    return contentsControl.query();
  }

  /**
   * End the Flow :(
   * waitForEnd waits until all core have consumed all items
   */
  @Override
  public void end(boolean waitForEnd) {
    this.isEnd = true;
    try {
      if (waitForEnd)
        while (!this.hasEnded() || !this.contentsControl.hasEnded()) {
          Thread.currentThread().sleep(100);
        }

    } catch (InterruptedException e) {
    }
    return;
  }

  /**
   * Has the Flow ended
   *
   * @return
   */
  @Override
  public boolean hasEnded() {
    return this.isEnd && this.size() == 0;
  }

  /**
   * Return the index field name
   *
   * @return
   */
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

  public FlowItem[] takeAll() {
    FlowItem[] result;
    waitForInput();
    /*int mmsize = this.item_queue.size();
    if ((getNumberOfNewItems(mmsize, last_queue_size)) > 0) {
      result = Arrays
          .copyOfRange(this.item_queue.toArray(this.itemqueue_out), this.last_queue_size, mmsize);
      this.last_queue_size = mmsize;
      clearStreamWhenFull();
    } else { // no domain
      result = EmptyItemArray.EMPTY_ITEM_ARRAY;
    }*/

    return null;
  }

  private boolean addToStreamWithNoBlock(FlowItem item, boolean immediately) {
    boolean put_ok;
    this.flush = false;
    if (put_ok = this.item_queue.offer(item)) {
      if (immediately) {
        this.flush = true;
        synchronized (lock) {
          lock.notify();
        }
      }
    } else {
      while (!put_ok && !hasEnded()) {
        try {
          put_ok = this.item_queue.offer(item, 1, TimeUnit.MILLISECONDS);
          if (!put_ok) {
            this.flush = true;
            synchronized (lock) {
              lock.notify();
            }
            LockSupport.parkNanos(LOCK_PARK_NANOS);
          }
        } catch (InterruptedException e) {
          LockSupport.parkNanos(LOCK_PARK_NANOS);
        }
      }
    }
    return put_ok;
  }

  private boolean addToStreamWithBlock(FlowItem<T> item, boolean immediately) {
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

  private int size() {
    return this.item_queue.size(); // - last_queue_size;
  }

  private void waitForInput() {
    int ticks = count_down_latch;
    FlowItem<T> head;
    while (!hasEnded()) {
      if (ticks == 0 || flush) {
        this.item_queue.drainTo(this.contentsControl.getItemRepositoryContents());
        ticks = count_down_latch;
      }
      ticks--;
      LockSupport.parkNanos(100000);
    }
  }

  private int getNumberOfNewItems(int mmsize, int lastsize) {
    return mmsize - lastsize;
  }

  private void clearStreamWhenFull() {
    if (this.last_queue_size == this.queue_max_size) {
      this.last_queue_size = 0;
      this.item_queue.clear();
    }
  }

  @Override
  public Flowable<T, R> countDownLatch(int count_down_latch) {
    this.count_down_latch = count_down_latch;
    return this;
  }
}
