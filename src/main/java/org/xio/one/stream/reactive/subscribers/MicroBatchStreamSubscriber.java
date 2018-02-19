package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.stream.Stream;

public abstract class MicroBatchStreamSubscriber<R,E> extends ContinuousStreamSubscriber<R,E> {

  @Override
  protected R process(Stream<Event<E>> e) {
    if (e != null) {
      return processStream(e);
    } else return null;
  }

  protected abstract R processStream(Stream<Event<E>> e);

}
