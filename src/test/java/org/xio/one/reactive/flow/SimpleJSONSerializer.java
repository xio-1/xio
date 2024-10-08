package org.xio.one.reactive.flow;


import java.io.IOException;
import java.util.Optional;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.logging.ItemDeserializer;
import org.xio.one.reactive.flow.domain.item.logging.ItemSerializer;

public class SimpleJSONSerializer<T> implements ItemSerializer<T>, ItemDeserializer<T> {

  @Override
  public byte[] serialize(Item<T> item, Optional<byte[]> delim) {
    if (delim.isEmpty())
        return (JSONUtil.toJSONString(item)).getBytes();
    byte[] json = JSONUtil.toJSONString(item).getBytes();
    byte[] combined = new byte[json.length + delim.get().length];
    System.arraycopy(json,0,combined,0         ,json.length);
    System.arraycopy(delim.get(),0,combined,json.length,delim.get().length);
    return combined;

  }

  @Override
  public Item<T> deserialize(byte[] input, Optional<byte[]> delimiter) {
    String inputJSON = new String(input);
    if (delimiter.isPresent())
      inputJSON.replaceAll(new String(delimiter.get()),"");
    try {
      return (Item<T>) JSONUtil.fromJSONString(inputJSON, ItemPojo.class).toItem();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
