package org.xio.one.reactive.flow.events;

import java.util.Comparator;

/**
 * Created by Admin on 09/09/2014.
 */
public class EventIDComparator implements Comparator<Event> {
  @Override
  public int compare(Event o1, Event o2) {
    if (o1 == o2)
      return 0;
    else if (o1.eventId() == o2.eventId())
      return 0;
    else if (o1.eventId() > o2.eventId())
      return 1;
    else if (o1.eventId() < o2.eventId())
      return -1;
    else
      return -1;
  }
}
