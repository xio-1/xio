package org.xio.one.reactive.flow;

import org.xio.one.reactive.util.ReactiveExecutors;
import org.xio.one.reactive.flow.events.Event;
import org.xio.one.reactive.flow.events.EventSequenceComparator;

import java.util.Arrays;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;

/**
 * The Xio.contents.events eventQueryStore where the putAll events are persisted in memory
 */
final class FlowControl<T> {

  protected volatile ConcurrentSkipListSet<Event<T>> eventRepositoryContents;
  protected volatile ConcurrentHashMap<Object, Event<T>> eventStoreIndexContents;
  private AsyncFlow eventStream = null;
  private boolean isEnd = false;
  private FlowContents eventStoreOperations = null;
  private String eventStoreIndexFieldName;

  /**
   * New Event BaseWorker Execution using the given checked Comparator</Event> to order the results
   * events TTL seconds will be retained before being automatically removed from the store
   */
  public FlowControl(AsyncFlow eventStream) {
    this.eventStream = eventStream;
    eventRepositoryContents = new ConcurrentSkipListSet<>(new EventSequenceComparator<>());
    eventStoreOperations = new FlowContents<Event<T>>(this, eventStream);
    eventStoreIndexContents = new ConcurrentHashMap<>();
    if (eventStream.indexFieldName() != null) {
      eventStoreIndexFieldName = eventStream.indexFieldName();
    }
    ReactiveExecutors.eventLoopThreadPoolInstance().submit(new ExpiredEventsCollector());
    ReactiveExecutors.eventLoopThreadPoolInstance().submit(new WorkerInput(this));
  }

  /**
   * Get the contents stores contents operations
   *
   * @return
   */
  public FlowContents<Event<T>> query() {
    return eventStoreOperations;
  }

  /**
   * Do work
   *
   * @param events
   */
  private Event work(Event[] events) {
    Arrays.stream(events).forEach(event -> {
      eventRepositoryContents.add(event);
      if (getEventStoreIndexFieldName() != null)
        eventStoreIndexContents.put(event.getFieldValue(getEventStoreIndexFieldName()), event);
    });
    if (events.length > 0)
      return events[events.length - 1];
    else
      return null;
  }

  public boolean hasEnded() {
    return this.isEnd;
  }


  public String getEventStoreIndexFieldName() {
    return eventStoreIndexFieldName;
  }

  public ConcurrentHashMap<Object, Event<T>> getEventStoreIndexContents() {
    return eventStoreIndexContents;
  }

  /**
   * Gets all the input from the Xio.contents.events eventStream and persists it to the contents store
   */
  private class WorkerInput implements Runnable {

    FlowControl eventStore;

    public WorkerInput(FlowControl eventStore) {
      this.eventStore = eventStore;
    }

    @Override
    public void run() {
      try {
        Event last = null;
        boolean hasRunatLeastOnce = false;
        while (!eventStream.hasEnded() || !hasRunatLeastOnce) {
          Event next_last = this.eventStore.work(eventStream.takeAll());
          if (next_last != null)
            last = next_last;
          hasRunatLeastOnce = true;
        }
        Event next_last = this.eventStore.work(eventStream.takeAll());
        if (next_last != null)
          last = next_last;
        if (last != null)
          while (!last.equals(this.eventStore.eventStoreOperations.last()) || (last.eventId()
              > getMinimumLastSeenProcessed(eventStream)))
            LockSupport.parkNanos(100000);
        eventStore.isEnd = true;
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
      }
    }
  }


  private long getMinimumLastSeenProcessed(AsyncFlow eventStream) {
    Map<String, Subscription> subscriptionMap = eventStream.getSubscriberSubscriptions();
    if (subscriptionMap.size() > 0) {
      OptionalLong lastSeenEventId = subscriptionMap.entrySet().stream()
          .mapToLong(e -> e.getValue().getLastSeenEvent().eventId()).min();
      if (lastSeenEventId.isPresent())
        return lastSeenEventId.getAsLong();
      else
        return Long.MAX_VALUE;
    } else
      return Long.MAX_VALUE;
  }

  /**
   * Removes seen dead events from the contents store
   */
  private class ExpiredEventsCollector implements Runnable {

    @Override
    public void run() {
      try {
        while (!eventStream.hasEnded()) {
          Thread.currentThread().sleep(1000);
          if (!eventRepositoryContents.isEmpty()) {
            long lastSeenEventId = getMinimumLastSeenProcessed(eventStream);
            eventRepositoryContents.removeIf(event -> !event.isAlive(lastSeenEventId));
          }
        }
      } catch (Exception e) {

      }
    }

  }
}
