package org.xio.one.reactive.flow.domain.item.logging;

import java.io.IOException;
import java.util.Optional;
import org.xio.one.reactive.flow.domain.item.Item;

public interface ItemSerializer<T> {
  byte[] serialize(Item<T> item, Optional<byte[]> delimiter) throws IOException;
}
