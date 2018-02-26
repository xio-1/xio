package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class ContinuousSingleEventSubscriber<R, E> extends ContinuousStreamSubscriber<Boolean,E> {

  @Override
  public void initialise() {
  }

  @Override
  protected Boolean process(Stream<Event<E>> e) {
    e.forEach(event-> process(event.value()));
    return true;
  }

  public abstract Boolean process(E eventValue);

}
