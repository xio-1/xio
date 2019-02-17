package org.xio.one.reactive.flow.subscribers.internal.functional;

@FunctionalInterface
public interface OnEndReturnFunction<R> {
  R onEnd();
}
