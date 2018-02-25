package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.ArrayList;
import java.util.stream.Stream;

public abstract class NextSingleEventSubscriber<R, E> extends BaseSubscriber<R, E> {

  private int currentIndex = -1;
  private ArrayList<E> eventHistoryList;;

  @Override
  public void initialise() {
    eventHistoryList = new ArrayList<>();
  }

  @Override
  protected R process(Stream<Event<E>> e) {
    if (e != null) {
      e.forEach(event -> eventHistoryList.add(event.value()));
      currentIndex++;
      this.stop();
      return process(eventHistoryList.get(currentIndex));
    } else if (currentIndex < eventHistoryList.size() - 1) {
      currentIndex++;
      this.stop();
      return process(eventHistoryList.get(currentIndex));
    } else return null;
  }

  public abstract R process(E eventValue);
}
