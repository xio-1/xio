package org.xio.one.reactive.flow.domain.flow;

public interface FutureItemFlowable<T, R> extends Flowable<T, R> {

  Promise<R> submitItemWithTTL(long ttlSeconds, T value);

  Promise<R> submitItem(T value);

}
