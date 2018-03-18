package org.xio.one.reactive.flow.core;

public interface Callback<R> {
  void handleResult(R result);
  void handleError(Throwable e, Object source);
}
