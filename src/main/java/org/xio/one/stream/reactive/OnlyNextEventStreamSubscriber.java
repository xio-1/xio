package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;

import java.util.ArrayList;
import java.util.stream.Stream;

public abstract class OnlyNextEventStreamSubscriber<E> extends BaseSubscriber<E> {

  private int currentIndex = -1;
  private ArrayList<Event> resultArrayList;;

  @Override
  public void initialise() {
    resultArrayList = new ArrayList<>();
  }

  @Override
  protected E process(Stream<Event> e) {
    if (e != null) {
      e.forEach(event -> resultArrayList.add(event));
      currentIndex++;
      this.stop();
      return process((E) resultArrayList.get(currentIndex).getEventValue());
    } else if (currentIndex < resultArrayList.size() - 1) {
      currentIndex++;
      this.stop();
      return process((E) resultArrayList.get(currentIndex).getEventValue());
    } else return null;
  }

  public abstract E process(E eventValue);
}
