package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;

import java.util.ArrayList;
import java.util.stream.Stream;

public abstract class StreamSubscriber<R> extends BaseSubscriber<Stream<R>> {

  private ArrayList<R> resultArrayList = new ArrayList<>();

  @Override
  public void initialise() {
    resultArrayList = new ArrayList<>();
  }

  @Override
  protected Stream<R> process(Stream<Event> e) {
    e.forEach(event->resultArrayList.add(process((R) event.getEventValue())));
    return resultArrayList.stream();
  }

  public abstract R process(R eventValue);

}
