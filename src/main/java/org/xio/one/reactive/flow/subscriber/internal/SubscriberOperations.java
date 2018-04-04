package org.xio.one.reactive.flow.subscriber.internal;

import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.NavigableSet;

public interface SubscriberOperations<R,E> {

  void emit(NavigableSet<FlowItem<E>> e);

  boolean stop();

  boolean isDone();

  R peek();

  R getNext();

  SubscriberOperations<R,E> getSubscriber();

  void initialise();

  void setResult(R result);

  R getResult();

  void finalise();

  void process(NavigableSet<FlowItem<E>> e);
}
