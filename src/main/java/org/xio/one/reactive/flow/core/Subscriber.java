package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.core.domain.FlowItem;

import java.util.NavigableSet;

public interface Subscriber<R,E> {

  void emit(NavigableSet<FlowItem<E>> e);

  boolean stop();

  boolean isDone();

  R peek();

  R getNext();

  Subscriber<R,E> getSubscriber();

  void initialise();

  void setResult(R result);

  R getResult();

  void finalise();

  void process(NavigableSet<FlowItem<E>> e);
}
