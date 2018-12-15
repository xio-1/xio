package org.xio.one.reactive.http.wee.event.platform.domain.selector;

import org.xio.one.reactive.http.wee.event.platform.domain.EmptyEvent;
import org.xio.one.reactive.http.wee.event.platform.domain.Event;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;


public class Selector {

  private ArrayList<FilterEntry> filterList = new ArrayList<>();

  public Selector() {
    super();
  }

  public ArrayList<FilterEntry> getFilterList() {
    return filterList;
  }

  public void addFilterEntry(FilterEntry filterEntry) {
    this.filterList.add(filterEntry);
  }

  public Event work(Event eventIn) {
    return work(eventIn, null);
  }

  public Event work(Event eventIn, Map<String, Object> params) {

    if (filterList.size() == 0)
      return eventIn;
    else {
    }
    if (filterList.stream().filter((e)-> e.getField().equals("")).map(e -> doFilter(eventIn, e)).collect(Collectors.toSet())
        .contains(eventIn))
      return eventIn;
    else
      return EmptyEvent.EMPTY_EVENT;
  }

  private Event doFilter(Event eventIn, FilterEntry filterEntry) {

    switch (filterEntry.getOperator()) {
      case IN:
        return doContains(eventIn, filterEntry.getField(), filterEntry.getValue());
      case EQ:
        return doEquals(eventIn, filterEntry.getField(), filterEntry.getValue());
    }
    return EmptyEvent.EMPTY_EVENT;
  }

  private Event doContains(Event event, String field, Object value) {
    if (event.getFieldValue(field) != null && event.getFieldValue(field).toString()
        .contains(value.toString()))
      return event;
    else
      return EmptyEvent.EMPTY_EVENT;
  }

  private Event doEquals(Event event, String field, Object value) {
    if (event.getFieldValue(field) != null && event.getFieldValue(field).toString()
        .equals(value.toString()))
      return event;
    else
      return EmptyEvent.EMPTY_EVENT;
  }
}
