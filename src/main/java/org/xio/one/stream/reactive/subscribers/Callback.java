package org.xio.one.stream.reactive.subscribers;

public interface Callback<R> {
  void processResult(R result);
}
