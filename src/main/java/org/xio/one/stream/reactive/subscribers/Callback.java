package org.xio.one.stream.reactive.subscribers;

public interface Callback<R> {
  void handleResult(R result);
  void handleError(Throwable e, Object source);
}
