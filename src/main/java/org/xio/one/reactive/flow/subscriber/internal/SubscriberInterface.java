package org.xio.one.reactive.flow.subscriber.internal;

import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.NavigableSet;

public interface SubscriberInterface<R, T> {

  void emit(NavigableSet<FlowItem<T, R>> e);

  boolean stop();

  boolean isDone();

  String getId();

  int delayMS();

  R getNext();

  void initialise();

  void setResult(R result);

  R getResult();

  void finalise();

  void process(NavigableSet<FlowItem<T, R>> e);
}
