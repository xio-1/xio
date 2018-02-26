package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class StreamSubscriber<R,E> extends BaseSubscriber<R,E> {

  @Override
  protected void process(Stream<Event<E>> e) {
    e.forEach(event-> callCallbacks(process(event.value())));
  }

  public abstract R process(E eventValue);

  @Override
  public void initialise() {

  }

  @Override
  public void finalise() {

  }
}
