package org.xio.one.reactive.flow.domain.flow;

import org.xio.one.reactive.flow.Promise;

public interface FutureItemFlowable<T, R> extends Flowable<T, R> {

  Promise<R> submitItemWithTTL(long ttlSeconds, T value);

  Promise<R> submitItem(T value);

}
