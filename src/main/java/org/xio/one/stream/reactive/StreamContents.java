package org.xio.one.stream.reactive;

import org.xio.one.stream.event.ComparitorEvent;
import org.xio.one.stream.event.Event;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static org.xio.one.stream.event.EmptyEvent.EMPTY_EVENT;

/** EventStoreOperations @Author Xio @Copyright Xio */
public final class StreamContents<T> {

  public final NavigableSet<Event<T>> EMPTY_EVENT_SET = new ConcurrentSkipListSet<>();
  private StreamRepository eventStore;
  private AsyncStream eventStream;
  private NavigableSet<Event<T>> eventStoreContents = null;

  public StreamContents(StreamRepository eventStore, AsyncStream eventStream) {
    this.eventStore = eventStore;
    this.eventStream = eventStream;
    this.eventStoreContents = (NavigableSet<Event<T>>) eventStore.eventRepositoryContents;
  }

  public Event<T> event(long id) {
    return eventStoreContents.higher(new ComparitorEvent(id - 1));
  }

  public Event event(Object index) {
    return (Event) eventStore.getEventStoreIndexContents().get(index);
  }

  public long count() {
    return eventStoreContents.size();
  }

  public Event[] all() {
    List<Event> events = eventStoreContents.parallelStream().collect(Collectors.toList());
    return events.toArray(new Event[events.size()]);
  }

  protected final NavigableSet<Event<T>> allAfter(Event lastEvent) {
    try {
      NavigableSet<Event<T>> querystorecontents =
          Collections.unmodifiableNavigableSet(this.eventStoreContents);
      if (lastEvent != null) {
        Event newLastEvent = querystorecontents.last();
        NavigableSet<Event<T>> events =
            Collections.unmodifiableNavigableSet(
                querystorecontents.subSet(lastEvent, false, newLastEvent, true));
        if (events.size() > 0) {
          Event newFirstEvent = events.first();
          if (newFirstEvent.eventId() == (lastEvent.eventId() + 1)) {
            // if last event is in correct sequence then
            if (newLastEvent.eventId() == newFirstEvent.eventId() + events.size() - 1)
              // if the size of the events to return is correct i.e. all in sequence
              if (events.size() == (newLastEvent.eventId() + 1 - newFirstEvent.eventId())) {
                return events;
              }
            return extractItemsThatAreInSequence(lastEvent, events, newFirstEvent);
          } else LockSupport.parkNanos(100000);
        } else LockSupport.parkNanos(100000);
      } else
        return extractItemsThatAreInSequence(
            EMPTY_EVENT, querystorecontents, querystorecontents.first());
    } catch (NoSuchElementException e) {
    } catch (IllegalArgumentException e2) {
    }
    return EMPTY_EVENT_SET;
  }

  private NavigableSet<Event<T>> extractItemsThatAreInSequence(
      Event lastEvent, NavigableSet<Event<T>> events, Event newFirstEvent) {
    Event[] events1 = events.toArray(new Event[events.size()]);
    int index = 0;
    Event last = lastEvent;
    Event current = events1[0];
    while (current.eventId() == last.eventId() + 1 && index <= events1.length) {
      last = current;
      index++;
      if (index < events1.length) current = events1[index];
    }
    return events.subSet(newFirstEvent, true, last, true);
  }

  public Event<T> last() {
    NavigableSet<Event<T>> querystorecontents =
        Collections.unmodifiableNavigableSet(this.eventStoreContents);
    if (querystorecontents.isEmpty()) return EMPTY_EVENT;
    else {
      return querystorecontents.last();
    }
  }

  public boolean hasEnded() {
    return eventStore.hasEnded();
  }

  /*public Event getLastSeenEventByFieldnameValue(String fieldname, Object value) {
    Event toReturn = eventStore.eventStoreIndexContents.get(new EventKey(fieldname, value));
    if (toReturn == null) {
      List<Event> events =
          eventRepositoryContents.parallelStream().filter(u -> u.isAlive(eventTTLSeconds))
              .filter(i -> i.getFieldValue(fieldname).toString().equals(value))
              .collect(Collectors.toList());
      if (events.size() > 0)
        return events.get(events.size() - 1);
    } else
      return toReturn;
    return EMPTY_EVENT;
  }*/

  public Event first() {
    return this.eventStoreContents.first();
  }

  public Optional<Event> nextFollowing(long eventid) {
    return Optional.ofNullable(this.eventStoreContents.higher(new ComparitorEvent(eventid)));
  }

  /*public Event getFirstBy(String fieldname, Object value) {
    List<Event> events =
        eventRepositoryContents.parallelStream().filter(u -> u.isAlive(eventTTLSeconds))
            .filter(i -> i.getFieldValue(fieldname).equals(value)).collect(Collectors.toList());
    if (events.size() > 0)
      return events.get(0);
    else
      return EMPTY_EVENT;
  }*/

  /*public double getAverage(String fieldname) {
    try {
      return this.eventRepositoryContents.parallelStream()
          .filter(u -> u.isAlive(eventTTLSeconds) && u.hasFieldValue(fieldname))
          .mapToDouble(e -> (double) e.getFieldValue(fieldname)).average().getAsDouble();
    } catch (NoSuchElementException e) {
      return 0.0d;
    }
  }*/

  /*public Event getMax(String fieldname) {
    try {

      Event event;
      event = eventRepositoryContents.parallelStream()
          .filter(u -> u.isAlive(eventTTLSeconds) && u.hasFieldValue(fieldname))
          .max(Comparator.comparing(i -> (double) i.getFieldValue(fieldname))).get();
      return event;
    } catch (NoSuchElementException e) {
      return EMPTY_EVENT;
    }
  }

  public Event getMin(String fieldname) {
    try {
      Event event;
      event = eventRepositoryContents.parallelStream()
          .filter(u -> u.isAlive(eventTTLSeconds) && u.hasFieldValue(fieldname))
          .min(Comparator.comparing(i -> (double) i.getFieldValue(fieldname))).get();
      return event;
    } catch (NoSuchElementException e) {
      return EMPTY_EVENT;
    }
  }

  public double getSum(String fieldname) {
    try {
      return this.eventRepositoryContents.parallelStream()
          .filter(u -> u.isAlive(eventTTLSeconds) && u.hasFieldValue(fieldname))
          .mapToDouble(e -> (double) e.getFieldValue(fieldname)).sum();
    } catch (NoSuchElementException e) {
      return 0.0d;
    }
  }

  public Set<Map.Entry<Object, Optional<Event>>> getLastSeenEventGroupedByFieldnameValue(
      String fieldname) {
    return eventRepositoryContents.parallelStream()
        .filter(u -> u.isAlive(eventTTLSeconds) && u.hasFieldValue(fieldname)).collect(
            Collectors.groupingBy(foo -> foo.getFieldValue(fieldname),
                Collectors.maxBy(new EventTimestampComparator()))).entrySet();

  }

    public Event[] getAllBy(String fieldName, Object value) {
    List<Event> events = eventRepositoryContents.parallelStream().filter(
        u -> u.isAlive(eventTTLSeconds) && u.getFieldValue(fieldName).toString().equals(value))
        .collect(Collectors.toList());
    return events.toArray(new Event[events.size()]);
  }

  */

  public NavigableSet<Event<T>> getTimeWindowSet(long from, long to)
      throws StreamException {
    // Get first and last events in the window
    // otherwise return empty set as nothing found in the window
    return EMPTY_EVENT_SET;
  }
}
