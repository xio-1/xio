package org.xio.one.reactive.flow.domain.item.logging;

import org.xio.one.reactive.flow.domain.item.Item;

public interface ItemLogger<T> {
  void logItem(Item<T> item);
  void close(boolean waitForEnd);
}
