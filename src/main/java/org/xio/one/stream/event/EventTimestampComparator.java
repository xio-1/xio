package org.xio.one.stream.event;

import java.util.Comparator;

/**
 * Created by Admin on 09/09/2014.
 */
public class EventTimestampComparator implements Comparator<Event> {
  @Override
  public int compare(Event o1, Event o2) {
    if (o1 == o2)
      return 0;
    else if (o1.getEventTimestamp() > o2.getEventTimestamp())
      return 1;
    else if (o1.getEventTimestamp() < o2.getEventTimestamp())
      return -1;
    else if (o1.getEventTimestamp() == o2.getEventTimestamp() && o1.getEventId() == (o2
        .getEventId()))
      return 0;
    else if (o1.getEventId() > o2.getEventId())
      return 1;
    else
      return -1;
  }
}
