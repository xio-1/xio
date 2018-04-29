package org.xio.one.reactive.flow.domain;

import org.xio.one.reactive.flow.domain.CompletionHandler;
import org.xio.one.reactive.flow.domain.Flowable;

public interface CompletableResultFlowable<T, R> extends Flowable<T,R> {

  void submitItem(T value, CompletionHandler<R, T> callbackHandler);

  void submitItemWithTTL(long ttlSeconds, T value, CompletionHandler<R, T> callbackHandler);

}
