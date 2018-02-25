package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;
import org.xio.one.stream.event.EventSequenceComparator;
import org.xio.one.stream.reactive.selector.Selector;
import org.xio.one.stream.reactive.util.AsyncStreamExecutor;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;

/** The Xio.contents.event eventQueryStore where the putAll events are persisted in memory */
final class StreamRepository<T> {

  protected volatile ConcurrentSkipListSet<Event<T>> eventRepositoryContents;
  protected volatile ConcurrentHashMap<Object, Event<T>> eventStoreIndexContents;
  private AsyncStream eventStream = null;
  private boolean isEnd = false;
  private Selector selector;
  private StreamContents eventStoreOperations = null;
  private String eventStoreIndexFieldName;

  /**
   * New Event BaseWorker Execution using the given checked Comparator</Event> to order the results
   * events TTL seconds will be retained before being automatically removed from the store
   *
   */
  public StreamRepository(AsyncStream eventStream, Selector selector) {
    this.eventStream = eventStream;
    if (selector != null) this.selector = selector;
    else this.selector = new Selector();
    eventRepositoryContents = new ConcurrentSkipListSet<>(new EventSequenceComparator<>());
    eventStoreOperations = new StreamContents<Event<T>>(this, eventStream);
    eventStoreIndexContents = new ConcurrentHashMap<>();
    if (eventStream.indexFieldName() != null) {
      eventStoreIndexFieldName = eventStream.indexFieldName();
    }
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
              eventRepositoryContents.add(event);
              if (getEventStoreIndexFieldName() != null)
                eventStoreIndexContents.put(event.getFieldValue(getEventStoreIndexFieldName()), event);
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

  public ConcurrentHashMap<Object, Event<T>> getEventStoreIndexContents() {
    return eventStoreIndexContents;
  }

  /**
   * Gets all the input from the Xio.contents.event eventStream and persists it to the contents store
   */
  private class WorkerInput implements Runnable {

    StreamRepository eventStore;

    public WorkerInput(StreamRepository eventStore) {
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
          while (!last.equals(this.eventStore.eventStoreOperations.last()))
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
        while (!eventStream.hasEnded()) {
          Thread.currentThread().sleep(1000);
          if (!eventRepositoryContents.isEmpty())
            eventRepositoryContents.removeIf(event -> !event.isAlive());
        }
      } catch (Exception e) {

      }
    }
  }
}
