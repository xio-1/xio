package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;
import org.xio.one.stream.selector.FilterEntry;
import org.xio.one.stream.event.EmptyEvent;
import org.xio.one.stream.selector.Selector;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SelectorCollectorSubscriber<E> extends BaseSubscriber<Event[]> {

  private ArrayList<Event> resultArrayList;
  private Selector selector = new Selector();
  ;

  public SelectorCollectorSubscriber() {
    super();
  }

  public SelectorCollectorSubscriber(FilterEntry filterEntry) {
    this.addFilterEntry(filterEntry);
  }

  public void addFilterEntry(FilterEntry filterEntry) {
    this.selector.addFilterEntry(filterEntry);
  }

  @Override
  public void initialise() {
    resultArrayList = new ArrayList<>();
  }

  @Override
  protected Event[] process(Stream<Event> e) {
    if (selector.getFilterList().size() > 0) {
      e.collect(Collectors.toList()).parallelStream().forEach(event -> {
        Event filteredEvent = selector.work(event, null);
        if (filteredEvent != EmptyEvent.EMPTY_EVENT) {
          resultArrayList.add(filteredEvent);
        }
      });
    } else
      resultArrayList.addAll(e.collect(Collectors.toList()));
    return resultArrayList.toArray(new Event[resultArrayList.size()]);
  }
}
