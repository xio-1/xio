package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.service.FlowContents;
import org.xio.one.reactive.flow.subscriber.FutureSubscriber;
import org.xio.one.reactive.flow.subscriber.MultiplexItemSubscriber;
import org.xio.one.reactive.flow.subscriber.SingleItemSubscriber;
import org.xio.one.reactive.flow.subscriber.internal.SubscriberInterface;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public interface Flowable<T, R> {

  String name();

  Flowable<T,R> addSubscriber(SubscriberInterface<R, T> subscriber);

  Flowable<T, R> enableImmediateFlushing();

  Flowable<T, R> executorService(ExecutorService executorService);

  Flowable<T,R> countDownLatch(int count_down_latch);

  long putItem(T value);

  long[] putItem(T... values);

  long[] putItemWithTTL(long ttlSeconds, T... values);

  Future<R> submitItemWithTTL(long ttlSeconds, T value);

  Future<R> submitItem(T value);

  FlowContents contents();

  void end(boolean waitForEnd);

  boolean hasEnded();


}
