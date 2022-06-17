package org.xio.one.reactive.flow.domain.flow;

public interface CompletableItemFlowable<T, R> extends Flowable<T, R> {

  void submitItem(T value, FlowItemCompletionHandler<R, T> completionHandler);

  void submitItemWithTTL(long ttlSeconds, T value,
      FlowItemCompletionHandler<R, T> completionHandler);

}
