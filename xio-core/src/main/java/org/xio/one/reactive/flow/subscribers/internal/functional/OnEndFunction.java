package org.xio.one.reactive.flow.subscribers.internal.functional;

@FunctionalInterface
public interface OnEndFunction<T,R> {
    void onEnd();
}
