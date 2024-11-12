package org.xio.one.reactive.flow.domain.flow;


import java.util.Map;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.RestorableSubscriber;
import org.xio.one.reactive.flow.domain.item.logging.ItemDeserializer;
import org.xio.one.reactive.flow.domain.item.logging.ItemLogger;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;


public interface ItemFlowable<T, R> extends Flowable<T, R> {

  Subscriber<R, T> addSubscriber(Subscriber<R, T> subscriber);

  void restoreAllSubscribers(Map<String, Map<String, Object>> subscriberContext, RestorableSubscriber<R, T> restorableSubscriber);

  void removeSubscriber(Subscriber<R, T> subscriber);

  Subscriber<R, T> getSubscriber(String id);

  long putItem(T value);

  long[] putItem(T... values);

  long[] putItemWithTTL(long ttlSeconds, T... values);

  void addItemLogger(ItemLogger<T> logger);

  void recoverItemsFromLog(String filename, ItemDeserializer<T> deserializer);
}
