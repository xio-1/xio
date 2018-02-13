package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.stream.Stream;

public interface Subscriber<E> {

  void emit(Stream<Event> e);

  boolean stop();

  boolean isDone();

  E peek();

  E getNext();

  Subscriber<E> getSubscriber();

  void initialise();
}
