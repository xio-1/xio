package org.xio.one.reactive.flow.domain;

public interface CompletableItemFlowable<T, R> extends Flowable<T,R> {

  void submitItem(T value, ItemCompletionHandler<R, T> completionHandler);

  void submitItemWithTTL(long ttlSeconds, T value, ItemCompletionHandler<R, T> completionHandler);

}
