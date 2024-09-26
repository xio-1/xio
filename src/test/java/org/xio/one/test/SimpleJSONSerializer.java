package org.xio.one.test;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.logging.ItemSerializer;

public class SimpleJSONSerializer<T> implements ItemSerializer<T> {

  @Override
  public byte[] serialize(Item<T> item) {
    return (JSONUtil.toJSONString(item) + "/r/n").getBytes();
  }
}
