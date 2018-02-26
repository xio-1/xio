/*
 * stream.java
 * Created on 14 October 2006, 10:20
 * Copyright Xio
 */
package org.xio.one.stream.reactive;

import org.xio.one.stream.event.EmptyEventArray;
import org.xio.one.stream.event.Event;
import org.xio.one.stream.event.EventIDSequence;
import org.xio.one.stream.event.JSONValue;
import org.xio.one.stream.reactive.selector.Selector;
import org.xio.one.stream.reactive.subscribers.*;
import org.xio.one.stream.reactive.util.AsyncStreamExecutor;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * An AsyncStream - a streamContents of information
 *
 * <p>AsyncStream is implemented with a Command Query Responsibility Segregation external objects,
 * json, map can be PUT into the streamContents that are asynchronously loaded in memory to a
 * contents store that is used to provide a sequenced view of the events to downstream subscribers
 * using a separate thread
 */
public final class AsyncStream<T, R> {

  // streamContents variables
  private StreamRepository<T> eventEventStore;

  // constants
  private final long tickstowait = 10;
  private final int queue_max_size = 1024 * Runtime.getRuntime().availableProcessors();

  // input parameters
  private String streamName;
  private String indexFieldName;
  private Selector worker;
  private Map<String, Object> workerParams;
  private Map<String, Future> subscriptions = new HashMap<>();
  Map<String, Subscription<R, T>> subscriptionMap = new HashMap<>();

  // Queue control
  private Event[] eventqueue_out;
  private BlockingQueue<Event> event_queue;
  private int last_queue_size = 0;
  private volatile boolean isEnd = false;
  private volatile boolean flush = false;
  private EventIDSequence eventIDSequence;

  // locks
  private final Object lock = new Object();
  private long slowDownNanos = 0;
  private boolean flushImmediately = false;
  private ExecutorService executorService =
      AsyncStreamExecutor.subscriberCachedThreadPoolInstance();

  /**
   * Construct a new ordered EventStream with the given selector
   *
   * @param streamName
   */
  public AsyncStream(
      String streamName, String indexFieldName, Selector worker, Map<String, Object> workerParams) {
    this.event_queue = new LinkedBlockingQueue<>(queue_max_size);
    this.eventqueue_out = new Event[this.queue_max_size];
    this.streamName = streamName;
    this.indexFieldName = indexFieldName;
    this.worker = worker;
    this.eventEventStore = new StreamRepository(this, worker);
    this.workerParams = workerParams;
    this.eventIDSequence = new EventIDSequence();
  }

  /**
   * Construct a new ordered EventStream
   *
   * @param streamName
   */
  public AsyncStream(String streamName) {
    this.event_queue = new LinkedBlockingQueue<>(queue_max_size);
    this.eventqueue_out = new Event[this.queue_max_size];
    this.streamName = streamName;
    this.indexFieldName = null;
    this.worker = new Selector();
    this.eventEventStore = new StreamRepository(this, worker);
    this.eventIDSequence = new EventIDSequence();
  }

  public AsyncStream(String streamName, String indexFieldName) {
    this.event_queue = new LinkedBlockingQueue<>(queue_max_size);
    this.eventqueue_out = new Event[this.queue_max_size];
    this.streamName = streamName;
    this.indexFieldName = indexFieldName;
    this.worker = new Selector();
    this.eventEventStore = new StreamRepository(this, worker);
    this.eventIDSequence = new EventIDSequence();
  }

  /**
   * The streams name
   *
   * @return
   */
  public String name() {
    return streamName;
  }

  /** Put a putValueForSingleSubscriber value into the contents */
  public long putValue(T value) {
    return putValueWithTTL(Long.MAX_VALUE,value);
  }

  /**
   * Puts list of values into the contents
   *
   * @param values
   * @return
   */
  public long[] putValue(T... values) {
    return putValueWithTTL(Long.MAX_VALUE, values);
  }

  /** Put a json string value into the contents Throws IOException if json string is invalid */
  public boolean putJSONValue(String jsonValue) throws IOException {
    if (jsonValue != null && !isEnd) {
      Event event = new JSONValue(jsonValue, eventIDSequence.getNext());
      addToStreamWithLock(event, flushImmediately);
      if (slowDownNanos > 0) LockSupport.parkNanos(slowDownNanos);
      return true;
    }
    return false;
  }

  /**
   * Put a json string value into the contents with ttlSeconds Throws IOException if json string is
   * invalid
   */
  public boolean putJSONValueWithTTL(long ttlSeconds,String jsonValue) throws IOException {
    if (jsonValue != null && !isEnd) {
      Event event = new JSONValue(jsonValue, eventIDSequence.getNext(), ttlSeconds);
      addToStreamWithLock(event, flushImmediately);
      if (slowDownNanos > 0) LockSupport.parkNanos(slowDownNanos);
      return true;
    }
    return false;
  }
  /** Put value to contents with ttlSeconds */
  public long putValueWithTTL(long ttlSeconds,T value) {
    if (value != null && !isEnd) {
      Event<T> event = new Event<>(value, eventIDSequence.getNext(), ttlSeconds);
      addToStreamWithLock(event, flushImmediately);
      if (slowDownNanos > 0) LockSupport.parkNanos(slowDownNanos);
      return event.eventId();
    }
    return -1;
  }

  /**
   * Puts list of values into the contents with ttlSeconds
   *
   * @param values
   * @return
   */
  public long[] putValueWithTTL(long ttlSeconds, T... values) {
    long[] ids = new long[values.length];
    if (values != null && !isEnd) {
      for (int i = 0; i < values.length; i++) {
        Event<T> event = new Event<>(values[i], eventIDSequence.getNext(), ttlSeconds);
        ids[i] = event.eventId();
        addToStreamWithLock(event, flushImmediately);
        if (slowDownNanos > 0) LockSupport.parkNanos(slowDownNanos);
      }
    }
    return ids;
  }

  /**
   * Asychronously processes putValueForSingleSubscriber this event value with the given subscriber
   *
   * @return
   */
  public Future<R> putValueForSingleSubscriber(T value, FutureSingleSubscriber<R, T> subscriber) {
    if (subscriptionMap.get(subscriber.getId()) == null) {
      Subscription<R, T> subscription = new Subscription<>(this, (Subscriber<R, T>) subscriber);
      subscriptionMap.put(subscriber.getId(), subscription);
      subscription.subscribe();
    }
    long eventId = putValue(value);
    if (eventId != -1) {
      return subscriber.register(eventId);
    } else return null;
  }

  /**
   * Asychronously processes putValueForSingleSubscriber this event value with the given microbatch subscriber
   *
   * @return
   */
  public Future<R> putValueForMicroBatchSubscriber(T value, FutureMicroBatchSubscriber<R, T> subscriber) {
    if (subscriptionMap.get(subscriber.getId()) == null) {
      Subscription<R, T> subscription = new Subscription<>(this, (Subscriber<R, T>) subscriber);
      subscriptionMap.put(subscriber.getId(), subscription);
      subscription.subscribe();
    }
    long eventId = putValue(value);
    if (eventId != -1) {
      return subscriber.register(eventId);
    } else return null;
  }

  /**
   * Return the contents operations for the contents store
   *
   * @return
   */
  public StreamContents contents() {
    return eventEventStore.query();
  }

  /** End the life of this streamContents :( */
  public void end(boolean waitForEnd) {
    this.isEnd = true;
    try {
      if (waitForEnd)
        while (!this.hasEnded() || !this.eventEventStore.hasEnded()) {
          Thread.currentThread().sleep(100);
        }

    } catch (InterruptedException e) {
    }
    return;
  }

  /**
   * Returns if the streamContents has been ended A streamContents ends once any remaining inputs
   * events have been processed
   *
   * @return
   */
  public boolean hasEnded() {
    return isEnd && size() == 0;
  }

  public AsyncStream<T, R> withImmediateFlushing() {
    this.flushImmediately = true;
    return this;
  }

  public AsyncStream<T, R> withExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  /**
   * Subscribe to the contents with the given subscriber
   *
   * @param subscriber
   * @return
   */
  public Future<R> withSubscriber(BaseSubscriber<R, T> subscriber) {
    if (subscriber != null && subscriptions.get(subscriber.getId()) == null) {
      Subscription<R, T> subscription = new Subscription<>(this, subscriber);
      subscriptions.put(subscriber.getId(), subscription.subscribe());
    }
    return subscriptions.get(subscriber.getId());
  }

  public ExecutorService executorService() {
    return executorService;
  }

  public String indexFieldName() {
    return indexFieldName;
  }

  protected void slowdown() {
    slowDownNanos = slowDownNanos + 100000;
  }

  protected void speedup() {
    if (slowDownNanos > 0) slowDownNanos = 0;
  }

  protected long getSlowDownNanos() {
    return slowDownNanos;
  }

  protected Object lock() {
    return lock;
  }

  protected Event[] takeAll() {
    Event[] result;
    waitForInput();
    int mmsize = this.event_queue.size();
    if ((getNumberOfNewEvents(mmsize, last_queue_size)) > 0) {
      result =
          Arrays.copyOfRange(
              this.event_queue.toArray(this.eventqueue_out), this.last_queue_size, mmsize);
      this.last_queue_size = mmsize;
      clearStreamWhenFull();
    } else { // no events
      result = EmptyEventArray.EMPTY_EVENT_ARRAY;
    }

    return result;
  }

  private boolean addToStreamWithNoLock(Event event, boolean immediately) {
    boolean put_ok;
    this.flush = false;
    if (put_ok = this.event_queue.offer(event)) {
      if (immediately) {
        this.flush = true;
        synchronized (lock) {
          lock.notify();
        }
      }
    } else {
      while (!put_ok && !hasEnded()) {
        try {
          put_ok = this.event_queue.offer(event, 1, TimeUnit.MILLISECONDS);
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

  private boolean addToStreamWithLock(Event event, boolean immediately) {
    boolean put_ok = false;
    this.flush = false;
    while (!put_ok && !hasEnded())
      try {
        synchronized (lock) {
          if (put_ok = this.event_queue.add(event)) {
            if (immediately) {
              this.flush = true;
              lock.notify();
            }
          } else {
            this.flush = true;
            lock.notify();
          }
        }
        if (!put_ok) LockSupport.parkNanos(100000);
      } catch (Exception e) {
        this.flush = true;
        LockSupport.parkNanos(100000);
      }

    return put_ok;
  }

  private int size() {
    return event_queue.size() - last_queue_size;
  }

  private void waitForInput() {
    int ticks = 0;
    while (this.event_queue.remainingCapacity() != 0
        && !hasEnded()
        && ticks <= tickstowait
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

  private int getNumberOfNewEvents(int mmsize, int lastsize) {
    return mmsize - lastsize;
  }

  private void clearStreamWhenFull() {
    if (this.last_queue_size == this.queue_max_size) {
      this.last_queue_size = 0;
      this.event_queue.clear();
    }
  }
}
