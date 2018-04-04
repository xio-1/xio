package org.xio.one.reactive.flow.subscribers;

public interface Callback<R> {
  void handleResult(R result);
  void handleError(Throwable e, Object source);
}
