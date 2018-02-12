package org.xio.one.stream.store;

import org.xio.one.stream.AsyncStream;
import org.xio.one.stream.event.Event;
import org.xio.one.stream.event.EventSequenceComparator;
import org.xio.one.stream.event.WatermarkEvent;
import org.xio.one.stream.reactive.AsyncStreamExecutor;
import org.xio.one.stream.selector.Selector;

import java.util.Arrays;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;

/** The Xio.stream.event eventQueryStore where the putAll events are persisted in memory */
public class EventStore<T> {

  protected volatile ConcurrentSkipListSet<Event<T>> eventStoreContents;
  protected volatile ConcurrentHashMap<Object, Event> eventStoreIndexContents;
  protected int eventTTLSeconds = 0;
  private AsyncStream eventStream = null;
  private boolean isEnd = false;
  private Selector selector;
  private StreamContents eventStoreOperations = null;
  private String eventStoreIndexFieldName;

  /**
   * New Event BaseWorker Execution using the given checked Comparator</Event> to order the results
   * passing Xio.stream.event time to live > 0 when using the EventTimestampComparator specifies how
   * long events in seconds will be retained before being automatically removed from the baseWorker
   * results
   *
   * @param eventStream
   * @param eventTTLSeconds
   */
  public EventStore(AsyncStream eventStream, Selector selector, int eventTTLSeconds) {
    this.eventStream = eventStream;
    if (selector != null) this.selector = selector;
    else this.selector = new Selector();
    this.eventTTLSeconds = eventTTLSeconds;
    eventStoreContents = new ConcurrentSkipListSet<>(new EventSequenceComparator<>());
    eventStoreOperations = new StreamContents<Event<T>>(this, eventStream);
    eventStoreIndexContents = new ConcurrentHashMap<>();
    if (eventStream.getIndexFieldName() != null) {
      eventStoreIndexFieldName = eventStream.getIndexFieldName();
    }
    if (eventTTLSeconds > 0)
      AsyncStreamExecutor.eventLoopThreadPoolInstance().submit(new ExpiredEventsCollector());
    AsyncStreamExecutor.eventLoopThreadPoolInstance().submit(new WorkerInput(this));
  }

  /**
   * Get the contents stores contents operations
   *
   * @return
   */
  public StreamContents query() {
    return eventStoreOperations;
  }

  /**
   * Do work
   *
   * @param events
   */
  private Event work(Event[] events) {
    Arrays.stream(events)
        .forEach(
            event -> {
              eventStoreContents.add(event);
              if (event.getIndexKeyValue() != null)
                eventStoreIndexContents.put(event.getIndexKeyValue(), event);
            });
    if (events.length > 0) return events[events.length - 1];
    else return null;
  }

  public boolean hasEnded() {
    return this.isEnd;
  }

  /**
   * Call the baseWorker
   *
   * @param event
   * @return
   */
  private Event callWorker(Event event, Map<String, Object> workerParams) {
    Event toReturn = this.selector.work(event, workerParams);
    return toReturn;
  }

  public String getEventStoreIndexFieldName() {
    return eventStoreIndexFieldName;
  }

  public ConcurrentHashMap<Object, Event> getEventStoreIndexContents() {
    return eventStoreIndexContents;
  }

  /**
   * Gets all the input from the Xio.stream.event eventStream and persists it to the contents store
   */
  private class WorkerInput implements Runnable {

    EventStore eventStore;

    public WorkerInput(EventStore eventStore) {
      this.eventStore = eventStore;
    }

    @Override
    public void run() {
      try {
        Event last = null;
        boolean hasRunatLeastOnce = false;
        while (!eventStream.hasEnded() || !hasRunatLeastOnce) {
          Event next_last = this.eventStore.work(eventStream.takeAll());
          if (next_last != null) last = next_last;
          hasRunatLeastOnce = true;
        }
        Event next_last = this.eventStore.work(eventStream.takeAll());
        if (next_last != null) last = next_last;
        if (last != null)
          while (!last.equals(this.eventStore.eventStoreOperations.getLast()))
            LockSupport.parkNanos(100000);
        eventStore.isEnd = true;
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
      }
    }
  }

  /** Removes dead events from the contents store */
  private class ExpiredEventsCollector implements Runnable {

    @Override
    public void run() {
      try {
        Thread.currentThread().sleep(1000);
        while (!eventStream.hasEnded()) {
          Thread.currentThread().sleep(1000);
          removeExpiredEventsWhenLimitReached();
        }
      } catch (Exception e) {

      }
    }

    private Event createWatermarkTimestampEventForComparison() {
      return new WatermarkEvent(0, System.currentTimeMillis() - (eventTTLSeconds * 1000) - 1000);
    }

    private void removeExpiredEventsWhenLimitReached() {
      if (!eventStoreContents.isEmpty()) {
        Event watermarktimestampevent = createWatermarkTimestampEventForComparison();
        NavigableSet<Event> expiredevents =
            getEventsBelowWaterMarkTimestmapEvent(watermarktimestampevent);
        eventStoreContents.removeAll(expiredevents);
      }
    }

    private NavigableSet<Event> getEventsBelowWaterMarkTimestmapEvent(Event watermarkevent) {
      return eventStoreContents.headSet(watermarkevent, false);
    }
  }
}
