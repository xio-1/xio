/*
 * stream.java
 * Created on 14 October 2006, 10:20
 * Copyright Xio
 */
package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.domain.EmptyItemArray;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.domain.ItemIdSequence;
import org.xio.one.reactive.flow.domain.ItemJSONValue;
import org.xio.one.reactive.flow.service.FlowContents;
import org.xio.one.reactive.flow.service.FlowService;
import org.xio.one.reactive.flow.subscribers.FutureSubscriberBase;
import org.xio.one.reactive.flow.subscribers.MultiplexItemSubscriber;
import org.xio.one.reactive.flow.subscribers.SingleItemSubscriber;
import org.xio.one.reactive.flow.subscribers.Subscription;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * An Flow - a aFlowable stream of items
 * <p>
 * <p>Flow is implemented with a Command Query Responsibility Segregation external objects,
 * json etc can be put into the aFlowable and are then asynchronously loaded in memory to a
 * contents store that is used to provide a sequenced view of the flowing items to downstream
 * subscribers
 */
public final class Flow<T, R> implements Flowable<T, R> {

  // streamContents variables
  private FlowService<T> contentsControl;

  // constants
  private final long count_down_latch = 10;
  private final int queue_max_size = 1024 * Runtime.getRuntime().availableProcessors();

  // input parameters
  private String name;
  private String indexFieldName;

  public static final long DEFAULT_TIME_TO_LIVE_SECONDS = 10;
  private long defaultTTLSeconds = DEFAULT_TIME_TO_LIVE_SECONDS;


  private Map<String, Future> streamSubscriberFutureResultMap = new HashMap<>();
  private Map<String, Subscription<R, T>> subscriberSubscriptions = new HashMap<>();

  // Queue control
  private FlowItem[] itemqueue_out;
  private BlockingQueue<FlowItem> item_queue;
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
    this.item_queue = new LinkedBlockingQueue<>(queue_max_size);
    this.itemqueue_out = new FlowItem[this.queue_max_size];
    this.name = name;
    this.indexFieldName = indexFieldName;
    if (ttlSeconds > 0)
      this.defaultTTLSeconds = ttlSeconds;
    this.contentsControl = new FlowService<>(this);
    this.itemIDSequence = new ItemIdSequence();
    this.flushImmediately = flushImmediately;

  }

  /**
   * Add a single subscriber
   *
   * @param subscriber
   * @return
   */
  public Flowable<T, R> addSingleSubscriber(SingleItemSubscriber<R, T> subscriber) {
    if (subscriber != null && streamSubscriberFutureResultMap.get(subscriber.getId()) == null) {
      Subscription<R, T> subscription = new Subscription<>(this, subscriber);
      streamSubscriberFutureResultMap.put(subscriber.getId(), subscription.subscribe());
      subscriberSubscriptions.put(subscriber.getId(), subscription);
    }
    return this;
  }

  /**
   * Add a multiplex subscriber
   *
   * @param subscriber
   * @return
   */
  public Flowable<T, R> addMultiplexSubscriber(MultiplexItemSubscriber<R, T> subscriber) {
    if (subscriber != null && streamSubscriberFutureResultMap.get(subscriber.getId()) == null) {
      Subscription<R, T> subscription = new Subscription<R, T>(this, subscriber);
      streamSubscriberFutureResultMap.put(subscriber.getId(), subscription.subscribe());
      subscriberSubscriptions.put(subscriber.getId(), subscription);
    }
    return this;
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
  public Flowable<T, R> withExecutorService(ExecutorService executorService) {
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
    return putItemWithTTL(defaultTTLSeconds, value);
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
   * Put a json string value into the contents Throws IOException if json string is invalid
   */

  @Override
  public boolean putJSONItem(String jsonValue) throws IOException {
    if (jsonValue != null && !isEnd) {
      FlowItem item = new ItemJSONValue(jsonValue, itemIDSequence.getNext(), defaultTTLSeconds);
      addToStreamWithLock(item, flushImmediately);
      if (slowDownNanos > 0)
        LockSupport.parkNanos(slowDownNanos);
      return true;
    }
    return false;
  }

  /**
   * Put a json string value into the contents with ttlSeconds Throws IOException if json string is
   * invalid
   */

  @Override
  public boolean putJSONItemWithTTL(long ttlSeconds, String jsonValue) throws IOException {
    if (jsonValue != null && !isEnd) {
      FlowItem item = new ItemJSONValue(jsonValue, itemIDSequence.getNext(), ttlSeconds);
      addToStreamWithLock(item, flushImmediately);
      if (slowDownNanos > 0)
        LockSupport.parkNanos(slowDownNanos);
      return true;
    }
    return false;
  }

  /**
   * Put value to contents with ttlSeconds
   */
  @Override
  public long putItemWithTTL(long ttlSeconds, T value) {
    if (value != null && !isEnd) {
      FlowItem<T> item = new FlowItem<>(value, itemIDSequence.getNext(), ttlSeconds);
      addToStreamWithLock(item, flushImmediately);
      if (slowDownNanos > 0)
        LockSupport.parkNanos(slowDownNanos);
      return item.itemId();
    }
    return -1;
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
    if (values != null && !isEnd) {
      for (int i = 0; i < values.length; i++) {
        FlowItem<T> item = new FlowItem<>(values[i], itemIDSequence.getNext(), ttlSeconds);
        ids[i] = item.itemId();
        addToStreamWithLock(item, flushImmediately);
        if (slowDownNanos > 0)
          LockSupport.parkNanos(slowDownNanos);
      }
    }
    return ids;
  }

  /**
   * Each putItem is processed as a Future<R> using the given a single flow or multiplexed flow future subscriber
   * A future result will be made available immediately the domain is processed by the subscriber
   *
   * @return
   */
  @Override
  public Future<R> putItemWithTTL(long ttlSeconds, T value, FutureSubscriberBase<R, T> subscriber) {
    generateSubscription(subscriber);
    return putAndReturnAsCompletableFuture(ttlSeconds, value, subscriber);
  }

  private void generateSubscription(FutureSubscriberBase<R, T> subscriber) {
    if (subscriberSubscriptions.get(subscriber.getId()) == null) {
      Subscription<R, T> subscription = new Subscription<>(this, subscriber);
      subscriberSubscriptions.put(subscriber.getId(), subscription);
      subscription.subscribe();
    }
  }

  private Future<R> putAndReturnAsCompletableFuture(long ttlSeconds, T value,
      FutureSubscriberBase<R, T> subscriber) {
    long itemId = putItemWithTTL(ttlSeconds, value);
    CompletableFuture<R> completableFuture = new CompletableFuture<>();
    if (itemId != -1) {
      return subscriber.register(itemId, completableFuture);
    } else
      return null;
  }

  /**
   * Each putItem is processed as a Future<R> using the given single or multiplex future subscriber
   * A future result will be made available immediately the domain is processed by the subscriber
   *
   * @return
   */
  @Override
  public Future<R> putItem(T value, FutureSubscriberBase<R, T> subscriber) {
    return putItemWithTTL(defaultTTLSeconds, value, subscriber);
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
    slowDownNanos = slowDownNanos + 100000;
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
    int mmsize = this.item_queue.size();
    if ((getNumberOfNewItems(mmsize, last_queue_size)) > 0) {
      result = Arrays
          .copyOfRange(this.item_queue.toArray(this.itemqueue_out), this.last_queue_size, mmsize);
      this.last_queue_size = mmsize;
      clearStreamWhenFull();
    } else { // no domain
      result = EmptyItemArray.EMPTY_ITEM_ARRAY;
    }

    return result;
  }

  private boolean addToStreamWithNoLock(FlowItem item, boolean immediately) {
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
            LockSupport.parkNanos(100000);
          }
        } catch (InterruptedException e) {
          LockSupport.parkNanos(100000);
        }
      }
    }
    return put_ok;
  }

  private boolean addToStreamWithLock(FlowItem item, boolean immediately) {
    boolean put_ok = false;
    this.flush = false;
    while (!put_ok && !hasEnded())
      try {
        synchronized (lock) {
          if (put_ok = this.item_queue.add(item)) {
            if (immediately) {
              this.flush = true;
              lock.notify();
            }
          } else {
            this.flush = true;
            lock.notify();
          }
        }
        if (!put_ok)
          LockSupport.parkNanos(100000);
      } catch (Exception e) {
        this.flush = true;
        LockSupport.parkNanos(100000);
      }

    return put_ok;
  }

  private int size() {
    return item_queue.size() - last_queue_size;
  }

  private void waitForInput() {
    int ticks = 0;
    while (this.item_queue.remainingCapacity() != 0 && !hasEnded() && ticks <= count_down_latch
        && !flush) {
      try {
        synchronized (lock) {
          lock.wait(1);
        }
      } catch (InterruptedException e) {
        break;
      }
      ticks++;
    }
    flush = false;
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
}
