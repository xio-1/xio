package org.xio.one.reactive.flow.domain;

import java.util.concurrent.Future;

public interface FutureResultFlowable<T, R> extends Flowable<T,R> {

  Future<R> submitItemWithTTL(long ttlSeconds, T value);

  Future<R> submitItem(T value);

}
