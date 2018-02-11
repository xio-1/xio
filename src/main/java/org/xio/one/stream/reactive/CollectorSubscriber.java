package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectorSubscriber extends BaseSubscriber<Event[]> {

  private ArrayList<Event> resultArrayList;

  @Override
  public void initialise() {
    resultArrayList = new ArrayList<>();
  }

  @Override
  protected Event[] process(Stream<Event> e) {
    resultArrayList.addAll(e.collect(Collectors.toList()));
    return resultArrayList.toArray(new Event[resultArrayList.size()]);
  }
}
