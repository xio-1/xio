package org.xio.one.reactive.flow.domain.item.logging;

import org.xio.one.reactive.flow.domain.item.Item;

public interface ItemSerializer<T> {

  byte[] serialize(Item<T> item);
}
