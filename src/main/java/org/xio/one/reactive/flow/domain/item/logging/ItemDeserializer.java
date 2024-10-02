package org.xio.one.reactive.flow.domain.item.logging;

import java.util.Optional;
import org.xio.one.reactive.flow.domain.item.Item;

public interface ItemDeserializer<T> {
  Item<T> deserialize(byte[] input, Optional<byte[]> delimiter);
}
