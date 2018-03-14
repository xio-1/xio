package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.stream.Stream;

public interface Subscriber<R,E> {

  void emit(Stream<Event<E>> e);

  boolean stop();

  boolean isDone();

  R peek();

  R getNext();

  Subscriber<R,E> getSubscriber();

  void initialise();

  void setResult(R result);

  void finalise();

  void process(Stream<Event<E>> e);
}
