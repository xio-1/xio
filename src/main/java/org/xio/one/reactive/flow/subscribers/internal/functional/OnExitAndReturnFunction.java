package org.xio.one.reactive.flow.subscribers.internal.functional;

@FunctionalInterface
public interface OnExitAndReturnFunction<R> {

  R onExit();
}
