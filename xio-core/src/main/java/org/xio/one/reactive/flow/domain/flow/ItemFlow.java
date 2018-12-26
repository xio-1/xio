package org.xio.one.reactive.flow.domain.flow;

import org.xio.one.reactive.flow.subscriber.Subscriber;


public interface ItemFlow<T, R> extends Flowable<T, R> {

  void addSubscriber(Subscriber<R, T> subscriber);

  void removeSubscriber(String id);

  long putItem(T value);

  long[] putItem(T... values);

  long[] putItemWithTTL(long ttlSeconds, T... values);

}
