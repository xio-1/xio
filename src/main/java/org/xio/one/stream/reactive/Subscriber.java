package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;

import java.util.stream.Stream;

public interface Subscriber<E> {

  void emit(Stream<Event> e);

  boolean stop();

  boolean isDone();

  E peek();

  E getNext();

  Subscriber<E> getSubscriber();
}
