package org.xio.one.stream.store;

import org.xio.one.stream.AsyncStream;
import org.xio.one.stream.event.Event;
import org.xio.one.stream.event.MaxTimestampEvent;
import org.xio.one.stream.event.MinTimestampEvent;
import org.xio.one.stream.reactive.InconsistentWindowException;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static org.xio.one.stream.event.EmptyEvent.EMPTY_EVENT;

/** EventStoreOperations @Author Xio @Copyright Xio */
public class StreamContents<T> {

  public final NavigableSet<Event> EMPTY_EVENT_SET = new ConcurrentSkipListSet<>();
  private int eventTTLSeconds;
  private EventStore eventStore;
  private AsyncStream eventStream;
  private NavigableSet<Event> eventStoreContents = null;

  public StreamContents(EventStore eventStore, AsyncStream eventStream) {
    this.eventStore = eventStore;
    this.eventTTLSeconds = eventStore.eventTTLSeconds;
    this.eventStream = eventStream;
    this.eventStoreContents = eventStore.eventStoreContents;
  }

  public Event getEvent(long id) {
    List<Event> events =
        eventStoreContents
            .parallelStream()
            .filter(u -> u.getEventId() == id)
            .collect(Collectors.toList());
    if (events.size() == 1) return events.get(0);
    else return null;
  }

  public long getCount() {
    return eventStoreContents.parallelStream().filter(u -> u.isEventAlive(eventTTLSeconds)).count();
  }

  public Event[] getAll() {
    List<Event> events =
        eventStoreContents
            .parallelStream()
            .filter(u -> u.isEventAlive(eventTTLSeconds))
            .collect(Collectors.toList());
    return events.toArray(new Event[events.size()]);
  }

  public NavigableSet<Event> getAllAfter(Event lastEvent) {
    try {
      NavigableSet<Event> querystorecontents =
          Collections.unmodifiableNavigableSet(this.eventStoreContents);
      if (lastEvent != null) {
        Event newLastEvent = querystorecontents.last();
        NavigableSet<Event> events =
            Collections.unmodifiableNavigableSet(
                querystorecontents.subSet(lastEvent, false, newLastEvent, true));
        if (events.size() > 0) {
          Event newFirstEvent = events.first();
          if (newFirstEvent.getEventId() == (lastEvent.getEventId() + 1)) {
            // if last event is in correct sequence then
            if (newLastEvent.getEventId() == newFirstEvent.getEventId() + events.size() - 1)
              // if the size of the events to return is correct i.e. all in sequence
              if (events.size() == (newLastEvent.getEventId() + 1 - newFirstEvent.getEventId())) {
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

  private NavigableSet<Event> extractItemsThatAreInSequence(
      Event lastEvent, NavigableSet<Event> events, Event newFirstEvent) {
    Event[] events1 = events.toArray(new Event[events.size()]);
    int index = 0;
    Event last = lastEvent;
    Event current = events1[0];
    while (current.getEventId() == last.getEventId() + 1 && index <= events1.length) {
      last = current;
      index++;
      if (index < events1.length) current = events1[index];
    }
    return events.subSet(newFirstEvent, true, last, true);
  }

  public Event getLast() {
    NavigableSet<Event> querystorecontents =
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
          eventStoreContents.parallelStream().filter(u -> u.isEventAlive(eventTTLSeconds))
              .filter(i -> i.getFieldValue(fieldname).toString().equals(value))
              .collect(Collectors.toList());
      if (events.size() > 0)
        return events.get(events.size() - 1);
    } else
      return toReturn;
    return EMPTY_EVENT;
  }*/

  public Event getFirst() {
    return this.eventStoreContents
        .parallelStream()
        .findFirst()
        .filter(u -> u.isEventAlive(eventTTLSeconds))
        .get();
  }

  public Optional<Event> getFirstAfter(long eventid) {
    return this.eventStoreContents
        .parallelStream()
        .filter(u -> u.isEventAlive(eventTTLSeconds))
        .filter(u -> u.getEventId() > eventid)
        .findFirst();
  }

  /*public Event getFirstBy(String fieldname, Object value) {
    List<Event> events =
        eventStoreContents.parallelStream().filter(u -> u.isEventAlive(eventTTLSeconds))
            .filter(i -> i.getFieldValue(fieldname).equals(value)).collect(Collectors.toList());
    if (events.size() > 0)
      return events.get(0);
    else
      return EMPTY_EVENT;
  }*/

  /*public double getAverage(String fieldname) {
    try {
      return this.eventStoreContents.parallelStream()
          .filter(u -> u.isEventAlive(eventTTLSeconds) && u.hasFieldValue(fieldname))
          .mapToDouble(e -> (double) e.getFieldValue(fieldname)).average().getAsDouble();
    } catch (NoSuchElementException e) {
      return 0.0d;
    }
  }*/

  /*public Event getMax(String fieldname) {
    try {

      Event event;
      event = eventStoreContents.parallelStream()
          .filter(u -> u.isEventAlive(eventTTLSeconds) && u.hasFieldValue(fieldname))
          .max(Comparator.comparing(i -> (double) i.getFieldValue(fieldname))).get();
      return event;
    } catch (NoSuchElementException e) {
      return EMPTY_EVENT;
    }
  }

  public Event getMin(String fieldname) {
    try {
      Event event;
      event = eventStoreContents.parallelStream()
          .filter(u -> u.isEventAlive(eventTTLSeconds) && u.hasFieldValue(fieldname))
          .min(Comparator.comparing(i -> (double) i.getFieldValue(fieldname))).get();
      return event;
    } catch (NoSuchElementException e) {
      return EMPTY_EVENT;
    }
  }

  public double getSum(String fieldname) {
    try {
      return this.eventStoreContents.parallelStream()
          .filter(u -> u.isEventAlive(eventTTLSeconds) && u.hasFieldValue(fieldname))
          .mapToDouble(e -> (double) e.getFieldValue(fieldname)).sum();
    } catch (NoSuchElementException e) {
      return 0.0d;
    }
  }

  public Set<Map.Entry<Object, Optional<Event>>> getLastSeenEventGroupedByFieldnameValue(
      String fieldname) {
    return eventStoreContents.parallelStream()
        .filter(u -> u.isEventAlive(eventTTLSeconds) && u.hasFieldValue(fieldname)).collect(
            Collectors.groupingBy(foo -> foo.getFieldValue(fieldname),
                Collectors.maxBy(new EventTimestampComparator()))).entrySet();

  }

    public Event[] getAllBy(String fieldName, Object value) {
    List<Event> events = eventStoreContents.parallelStream().filter(
        u -> u.isEventAlive(eventTTLSeconds) && u.getFieldValue(fieldName).toString().equals(value))
        .collect(Collectors.toList());
    return events.toArray(new Event[events.size()]);
  }

  */

  public NavigableSet<Event> getTimeWindowSet(long from, long to)
      throws InconsistentWindowException {
    // Get first and last events in the window
    MaxTimestampEvent event_from = new MaxTimestampEvent(from - 1);
    MinTimestampEvent event_to = new MinTimestampEvent(to + 1);
    NavigableSet<Event> querystorecontents =
        Collections.unmodifiableNavigableSet(this.eventStoreContents);
    Event firstEvent = querystorecontents.higher(event_from);
    Event lastEvent = querystorecontents.lower(event_to);

    // if no events event are higher or lower than the window
    // bounds then there is nothing to process in that window
    if (firstEvent == null || lastEvent == null) return EMPTY_EVENT_SET;

    // check that the actual events found are in the window bounds
    if (checkWindowBounds(event_from, event_to, firstEvent, lastEvent)) {
      return querystorecontents.subSet(firstEvent, true, lastEvent, true);
    }

    // otherwise return empty set as nothing found in the window
    return EMPTY_EVENT_SET;
  }

  private boolean checkWindowBounds(
      MaxTimestampEvent event_from, MinTimestampEvent event_to, Event firstEvent, Event lastEvent) {
    return firstEvent.getEventTimestamp() < event_to.getEventTimestamp()
        && firstEvent.getEventTimestamp() > event_from.getEventTimestamp()
        && lastEvent.getEventTimestamp() < event_to.getEventTimestamp()
        && lastEvent.getEventTimestamp() > event_from.getEventTimestamp();
  }
}
