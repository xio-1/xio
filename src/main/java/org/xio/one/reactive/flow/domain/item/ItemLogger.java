package org.xio.one.reactive.flow.domain.item;

public interface ItemLogger<T> {
  void logItem(Item<T> item);

  void close(boolean waitForEnd);
}
