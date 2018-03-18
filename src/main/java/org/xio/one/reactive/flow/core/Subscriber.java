package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.domain.Item;

import java.util.stream.Stream;

public interface Subscriber<R,E> {

  void emit(Stream<Item<E>> e);

  boolean stop();

  boolean isDone();

  R peek();

  R getNext();

  Subscriber<R,E> getSubscriber();

  void initialise();

  void setResult(R result);

  R getResult();

  void finalise();

  void process(Stream<Item<E>> e);
}
