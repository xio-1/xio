package org.xio.one.reactive.flow.subscriber.internal;

import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.NavigableSet;

public interface SubscriberInterface<R,E> {

  void emit(NavigableSet<FlowItem<E>> e);

  boolean stop();

  boolean isDone();

  R peek();

  String getId();

  R getNext();

  SubscriberInterface<R,E> getSubscriber();

  void initialise();

  void setResult(R result);

  R getResult();

  void finalise();

  void process(NavigableSet<FlowItem<E>> e);
}
