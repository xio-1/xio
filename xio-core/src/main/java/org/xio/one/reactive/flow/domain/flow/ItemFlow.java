package org.xio.one.reactive.flow.domain.flow;

import org.xio.one.reactive.flow.subscribers.internal.SubscriberInterface;


public interface ItemFlow<T, R> extends Flowable<T, R> {

  void addSubscriber(SubscriberInterface<R, T> subscriber);

  void removeSubscriber(SubscriberInterface<R, T> subscriber);

  SubscriberInterface<R,T> getSubscriber(String subscriberId);

  long putItem(T value);

  long[] putItem(T... values);

  long[] putItemWithTTL(long ttlSeconds, T... values);

}
