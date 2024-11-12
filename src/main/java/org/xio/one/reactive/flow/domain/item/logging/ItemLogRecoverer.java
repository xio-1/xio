package org.xio.one.reactive.flow.domain.item.logging;

import org.xio.one.reactive.flow.Flow;

public interface ItemLogRecoverer<T,R> {
  void recoverAllItems(Flow<T,R> flow, ItemDeserializer<T> deserializer);
}
