package org.xio.one.reactive.http.wee.event.platform.domain.selector;

import org.xio.one.reactive.http.wee.event.platform.domain.EmptyEvent;
import org.xio.one.reactive.http.wee.event.platform.domain.Event;

import java.math.BigDecimal;
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
    else if (filterList.size() == 1) {
      return doFilter(eventIn, filterList.get(0));
    } else if (filterList.stream().map(e -> doFilter(eventIn, e)).collect(Collectors.toList())
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
      case GT:
        return doGreaterThan(eventIn, filterEntry.getField(), filterEntry.getValue());
      case LT:
        return doLessThan(eventIn, filterEntry.getField(), filterEntry.getValue());
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
    if (event.getFieldValue(field) != null)
      if ((value instanceof String) && (event.getFieldValue(field) instanceof String) && event
          .getFieldValue(field).equals(value))
        return event;
      else if ((value instanceof Number) && (event.getFieldValue(field) instanceof Number))
        if (new BigDecimal(event.getFieldValue(field).toString())
            .compareTo(new BigDecimal(value.toString())) == 0)
          return event;

    return EmptyEvent.EMPTY_EVENT;
  }

  private Event doGreaterThan(Event event, String field, Object value) {
    if (event.getFieldValue(field) instanceof Number && value instanceof Number)
      if (new BigDecimal(event.getFieldValue(field).toString())
          .compareTo(new BigDecimal(value.toString())) > 0)
        return event;
    return EmptyEvent.EMPTY_EVENT;
  }

  private Event doLessThan(Event event, String field, Object value) {
    if (event.getFieldValue(field) instanceof Number && value instanceof Number)
      if (new BigDecimal(event.getFieldValue(field).toString())
          .compareTo(new BigDecimal(value.toString())) < 0)
        return event;
    return EmptyEvent.EMPTY_EVENT;
  }

}
