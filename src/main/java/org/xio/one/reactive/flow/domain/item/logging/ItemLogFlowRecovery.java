package org.xio.one.reactive.flow.domain.item.logging;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.Item;

public interface ItemLogFlowRecovery<T,R> {
  void recoverAllItem(Flow<T,R> flow, ItemDeserializer<T> deserializer);
}
