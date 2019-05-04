package org.xio.one.reactive.flow.domain.flow;


import org.xio.one.reactive.flow.subscribers.internal.Subscriber;


public interface ItemFlowable<T, R> extends Flowable<T, R> {

  Subscriber<R, T> addSubscriber(Subscriber<R, T> subscriber);

  void removeSubscriber(Subscriber<R, T> subscriber);

  long putItem(T value);

  long[] putItem(T... values);

  long[] putItemWithTTL(long ttlSeconds, T... values);

}
