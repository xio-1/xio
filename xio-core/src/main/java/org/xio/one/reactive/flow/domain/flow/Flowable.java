package org.xio.one.reactive.flow.domain.flow;

import org.xio.one.reactive.flow.service.FlowContents;

import java.util.concurrent.ExecutorService;

public interface Flowable<T, R> {

  String name();

  Flowable<T, R> enableImmediateFlushing();

  Flowable<T, R> executorService(ExecutorService executorService);

  Flowable<T, R> countDownLatch(int count_down_latch);

  FlowContents contents();

  void end(boolean waitForEnd);

  boolean hasEnded();

  int size();

  boolean isEmpty();

}
