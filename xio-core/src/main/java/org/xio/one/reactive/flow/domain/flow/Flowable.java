package org.xio.one.reactive.flow.domain.flow;

import org.xio.one.reactive.flow.FlowContents;

import java.util.concurrent.ExecutorService;

public interface Flowable<T, R> {

  String name();

  String uuid();

  Flowable<T, R> enableImmediateFlushing();

  Flowable<T, R> executorService(ExecutorService executorService);

  Flowable<T, R> countDownLatch(int count_down_latch);

  FlowContents contents();

  void close(boolean waitForEnd);

  boolean hasEnded();

  int size();

  boolean isEmpty();

  long ttl();

  boolean housekeep();

}
