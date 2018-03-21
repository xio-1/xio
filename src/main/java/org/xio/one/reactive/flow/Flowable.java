package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.core.*;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public interface Flowable<T, R> {

  String name();

  Flowable<T,R> addSingleSubscriber(SingleSubscriber<R, T> subscriber);

  Flowable<T,R> addMultiplexSubscriber(MultiplexSubscriber<R, T> subscriber);

  Flowable<T, R> enableImmediateFlushing();

  Flowable<T, R> withExecutorService(ExecutorService executorService);

  long putItem(T value);

  long[] putItem(T... values);

  boolean putJSONItem(String jsonValue) throws IOException;

  boolean putJSONItemWithTTL(long ttlSeconds, String jsonValue) throws IOException;

  long putItemWithTTL(long ttlSeconds, T value);

  long[] putItemWithTTL(long ttlSeconds, T... values);

  Future<R> putItemWithTTL(long ttlSeconds, T value, SingleFutureSubscriber<R, T> subscriber);

  Future<R> putItem(T value, SingleFutureSubscriber<R, T> subscriber);

  Future<R> putItemWithTTL(long ttlSeconds, T value, MultiplexFutureSubscriber<R, T> subscriber);

  Future<R> putItem(T value, MultiplexFutureSubscriber<R, T> subscriber);

  FlowContents contents();

  void end(boolean waitForEnd);

  boolean hasEnded();
}
