package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class ContinuousCollectingStreamSubscriber<R,E> extends ContinuousStreamSubscriber<List<R>,E> {

  private ArrayList<R> eventHistoryList = new ArrayList<>();

  @Override
  public void initialise() {
    eventHistoryList = new ArrayList<>();
  }

  @Override
  protected List<R> process(Stream<Event<E>> e) {
    e.forEach(event-> eventHistoryList.add(process(event.value())));
    return eventHistoryList;
  }

  public abstract R process(E eventValue);

}
