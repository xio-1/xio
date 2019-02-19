package org.xio.one.reactive.flow.domain.flow;


import org.xio.one.reactive.flow.subscribers.FunctionalItemSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;


public interface ItemFlow<T, R> extends Flowable<T, R> {

  Subscriber<R, T> addSubscriber(Subscriber<R, T> subscriber);

  void removeSubscriber(Subscriber<R, T> subscriber);

  FunctionalItemSubscriber<R, T> publish();

  long putItem(T value);

  long[] putItem(T... values);

  long[] putItemWithTTL(long ttlSeconds, T... values);

}
