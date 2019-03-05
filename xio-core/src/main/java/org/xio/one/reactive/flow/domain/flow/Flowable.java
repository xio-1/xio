package org.xio.one.reactive.flow.domain.flow;

import org.xio.one.reactive.flow.FlowContents;

public interface Flowable<T, R> {

  String name();

  String uuid();

  Flowable<T, R> enableImmediateFlushing();

  FlowContents contents();

  void close(boolean waitForEnd);

  boolean hasEnded();

  boolean isAtEnd();

  int size();

  boolean isEmpty();

  long maxTTLSeconds();

  boolean housekeep();

}
