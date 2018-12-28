package org.xio.one.reactive.flow.domain.flow;

import org.xio.one.reactive.flow.subscriber.internal.SubscriberInterface;


public interface ItemFlow<T, R> extends Flowable<T, R> {

  void addSubscriber(SubscriberInterface<R, T> subscriber);

  void removeSubscriber(SubscriberInterface<R, T> subscriber);

  long putItem(T value);

  long[] putItem(T... values);

  long[] putItemWithTTL(long ttlSeconds, T... values);

}
