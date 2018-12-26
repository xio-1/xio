package org.xio.one.reactive.flow.domain.flow;

import java.util.concurrent.Future;

public interface FutureItemResultFlowable<T, R> extends Flowable<T, R> {

  Future<R> submitItemWithTTL(long ttlSeconds, T value);

  Future<R> submitItem(T value);

}
