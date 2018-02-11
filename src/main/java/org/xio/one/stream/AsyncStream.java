/*
 * stream.java
 * Created on 14 October 2006, 10:20
 * Copyright Xio
 */
package org.xio.one.stream;

import org.xio.one.stream.event.EmptyEventArray;
import org.xio.one.stream.event.Event;
import org.xio.one.stream.event.EventIDSequence;
import org.xio.one.stream.event.JSONValue;
import org.xio.one.stream.reactive.BaseSubscriber;
import org.xio.one.stream.reactive.SingleSubscriber;
import org.xio.one.stream.reactive.Subscription;
import org.xio.one.stream.selector.FilterEntry;
import org.xio.one.stream.selector.Selector;
import org.xio.one.stream.store.EventStore;
import org.xio.one.stream.store.StreamContents;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * A EventStream - a streamContents of information
 *
 * <p>A EventStream is implemented with a Command Query Responsibility Segregation External Events
 * can be put into the streamContents and a segregated (in memory) contents store is used to provide a
 * processed (i.e. filtered, aggregated, joined) view of the events for the EventStream that are
 * pre-processed via the streamContents's contents store selector processor.
 */
public class AsyncStream<T,R> {

  // streamContents variables
  private EventStore<T> eventEventStore;

  // constants
  private final long tickstowait = 10;
  private final int queue_max_size = 1024 * Runtime.getRuntime().availableProcessors();

  // input parameters
  private String streamName;
  private String indexFieldName;
  private int eventTTL;
  private Selector worker;
  private Map<String, Object> workerParams;
  private Map<String, Future<R>> subscriptions = new HashMap<>();

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

  /**
   * Construct a new timeseries ordered EventStream with the given selector
   *
   * <p>
   *
   * <p>Event time to live is to be used with the timestamp comparator for auto removal of events
   * that have lived in the results set for longer than eventTTLSeconds seconds a value <=0 will
   * never remove results
   *
   * @param streamName
   * @param eventTTL
   */
  public AsyncStream(
      String streamName,
      String indexFieldName,
      Selector worker,
      Map<String, Object> workerParams,
      int eventTTL) {
    this.event_queue = new LinkedBlockingQueue<>(queue_max_size);
    this.eventqueue_out = new Event[this.queue_max_size];
    this.streamName = streamName;
    this.indexFieldName = indexFieldName;
    this.eventTTL = eventTTL;
    this.worker = worker;
    this.eventEventStore = new EventStore(this, worker, eventTTL);
    this.workerParams = workerParams;
    this.eventIDSequence = new EventIDSequence();
  }

  /**
   * Construct a new timeseries ordered EventStream with the default selector
   *
   * <p>
   *
   * <p>Event time to live is to be used with the timestamp comparator for auto removal of events
   * that have lived in the results set for longer than eventTTLSeconds seconds a value <=0 will
   * never remove results
   *
   * @param streamName
   * @param eventTTL
   */
  public AsyncStream(String streamName, int eventTTL) {
    this.event_queue = new LinkedBlockingQueue<>(queue_max_size);
    this.eventqueue_out = new Event[this.queue_max_size];
    this.streamName = streamName;
    this.indexFieldName = null;
    this.eventTTL = eventTTL;
    this.worker = new Selector();
    this.eventEventStore = new EventStore(this, worker, eventTTL);
    this.eventIDSequence = new EventIDSequence();
  }

  /**
   * The streams name
   *
   * @return
   */
  public String getStreamName() {
    return streamName;
  }

  /** End the life of this streamContents :( */
  public void end(boolean waitForEnd) {
    this.isEnd = true;
    try {
      if (waitForEnd)
        while (!this.hasEnded() && !this.eventEventStore.hasEnded()) {
          Thread.currentThread().sleep(100);
        }
    } catch (InterruptedException e) {
    }
    System.out.println("end");
  }

  public void reset() {
    if (isEnd == true) {
      this.event_queue = new LinkedBlockingQueue<>(queue_max_size);
      last_queue_size=0;
      this.eventqueue_out = new Event[this.queue_max_size];
      this.eventEventStore = new EventStore(this, worker, eventTTL);
      this.eventIDSequence = new EventIDSequence();
      this.isEnd = false;
    }
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

  /**
   * Return the contents operations for the contents store
   *
   * @return
   */
  public StreamContents contents() {
    return eventEventStore.query();
  }

  /**
   * Asychronously processes just this event value with the given subscriber
   *
   * @return
   */
  public Future<T> single(T value, SingleSubscriber subscriber) {
    long eventId = put(value, true);
    if (eventId != -1) {
      subscriber.initialise(eventId);
      return (new Subscription<T>(this, subscriber)).subscribe();
    } else return null;
  }

  /**
   * Subscribe to the stream with the given subscriber
   *
   * @param subscriber
   * @return
   */
  public Future<R> withSubscriber(BaseSubscriber<R> subscriber) {
    if (subscriber != null && subscriptions.get(subscriber.getId()) == null) {
      Subscription<R> subscription = new Subscription<>(this, subscriber);
      subscriptions.put(
          subscriber.getId(),subscription.subscribe());
    }
    return subscriptions.get(subscriber.getId());
  }

  /** Put a single value into the stream */
  public long put(T value, boolean flushImmediately) {
    if (value != null && !isEnd) {
      Event<T> event = new Event<>(value, eventIDSequence.getNext());
      addToStreamWithLock(event, flushImmediately);
      if (slowDownNanos > 0) LockSupport.parkNanos(slowDownNanos);
      return event.getEventId();
    }
    return -1;
  }

  /** Put a json string value into the stream Throws IOException if json string is invalid */
  public boolean putJSON(String jsonValue, boolean flushImmediately) throws IOException {
    if (jsonValue != null && !isEnd) {
      Event event = new JSONValue(jsonValue, eventIDSequence.getNext());
      addToStreamWithLock(event, flushImmediately);
      if (slowDownNanos > 0) LockSupport.parkNanos(slowDownNanos);
      return true;
    }
    return false;
  }

  /**
   * Add a filter operation to a streamContents field
   *
   * @param filterEntry
   */
  public void addFilterEntry(FilterEntry filterEntry) {
    this.worker.addFilterEntry(filterEntry);
  }

  /**
   * Return the contents store against which contents operations can be made
   *
   * @return
   */
  protected EventStore getEventEventStore() {
    return eventEventStore;
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

  public Event[] takeAll() {
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

  public Map<String, Object> getWorkerParams() {
    return workerParams;
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

  public void slowdown() {
    slowDownNanos = slowDownNanos + 100000;
  }

  public void speedup() {
    if (slowDownNanos > 0) slowDownNanos = 0;
  }

  public long getSlowDownNanos() {
    return slowDownNanos;
  }

  public String getIndexFieldName() {
    return indexFieldName;
  }

  public Object getLock() {
    return lock;
  }
}
