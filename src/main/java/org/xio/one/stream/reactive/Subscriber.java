package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;

import java.util.stream.Stream;

public interface Subscriber<E> extends SubscriberResult<E> {
  void emit(Stream<Event> e);

  boolean stop(boolean mayInterruptIfRunning);

  boolean isDone();
}
