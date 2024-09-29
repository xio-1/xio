package org.xio.one.test;


import java.util.Optional;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.logging.ItemSerializer;

public class SimpleJSONSerializer<T> implements ItemSerializer<T> {

  @Override
  public byte[] serialize(Item<T> item, Optional<byte[]> delim) {
    ItemToJSONPojo<T> itemS = new ItemToJSONPojo<>(item.getItemValue(), item.getItemId(),1392039323);
    if (delim.isEmpty())
        return (JSONUtil.toJSONString(itemS)).getBytes();
    byte[] json = JSONUtil.toJSONString(itemS).getBytes();
    byte[] combined = new byte[json.length + delim.get().length];
    System.arraycopy(json,0,combined,0         ,json.length);
    System.arraycopy(delim.get(),0,combined,json.length,delim.get().length);
    return combined;

  }
}
