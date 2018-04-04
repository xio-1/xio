package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.core.*;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public interface Flowable<T, R> {

  String name();

  Flowable<T,R> addSingleSubscriber(FlowItemSubscriber<R, T> subscriber);

  Flowable<T,R> addMultiplexSubscriber(FlowItemMultiplexSubscriber<R, T> subscriber);

  Flowable<T, R> enableImmediateFlushing();

  Flowable<T, R> withExecutorService(ExecutorService executorService);

  long putItem(T value);

  long[] putItem(T... values);

  boolean putJSONItem(String jsonValue) throws IOException;

  boolean putJSONItemWithTTL(long ttlSeconds, String jsonValue) throws IOException;

  long putItemWithTTL(long ttlSeconds, T value);

  long[] putItemWithTTL(long ttlSeconds, T... values);

  Future<R> putItemWithTTL(long ttlSeconds, T value, FutureSubscriber<R, T> subscriber);

  Future<R> putItem(T value, FutureSubscriber<R, T> subscriber);

  FlowContents contents();

  void end(boolean waitForEnd);

  boolean hasEnded();
}
