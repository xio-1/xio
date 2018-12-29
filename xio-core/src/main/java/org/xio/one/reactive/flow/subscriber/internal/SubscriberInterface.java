package org.xio.one.reactive.flow.subscriber.internal;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.NavigableSet;

public interface SubscriberInterface<R, T> {

  void emit(NavigableSet<Item<T, R>> e);

  boolean stop();

  boolean isDone();

  String getId();

  int delayMS();

  R getNext();

  void initialise();

  R getResult();

  void setResult(R result);

  void finalise();

  void process(NavigableSet<Item<T, R>> e);
}
