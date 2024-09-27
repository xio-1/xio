package org.xio.one.test.utils;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.logging.ItemSerializer;

public class SimpleJSONSerializer<T> implements ItemSerializer<T> {

  @Override
  public byte[] serialize(Item<T> item) {
    ItemToJSONPojo<T> itemS = new ItemToJSONPojo<>(item.getItemValue(), item.getItemId());
    return (JSONUtil.toJSONString(itemS) + "/r/n").getBytes();
  }
}
