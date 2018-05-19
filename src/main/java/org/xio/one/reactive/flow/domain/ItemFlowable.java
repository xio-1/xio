package org.xio.one.reactive.flow.domain;

import org.xio.one.reactive.flow.subscriber.Subscriber;


public interface ItemFlowable<T, R> extends Flowable<T, R> {

  void addSubscriber(Subscriber<R, T> subscriber);

  long putItem(T value);

  long[] putItem(T... values);

  long[] putItemWithTTL(long ttlSeconds, T... values);

}
