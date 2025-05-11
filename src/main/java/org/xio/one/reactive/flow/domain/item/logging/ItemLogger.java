package org.xio.one.reactive.flow.domain.item.logging;

import org.xio.one.reactive.flow.domain.item.Item;

public interface ItemLogger<V> {

  void logItem(Item<V> item);

  void close(boolean waitForEnd);
}
