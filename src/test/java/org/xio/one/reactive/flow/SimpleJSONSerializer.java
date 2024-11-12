package org.xio.one.reactive.flow;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.logging.ItemDeserializer;
import org.xio.one.reactive.flow.domain.item.logging.ItemSerializer;

public class SimpleJSONSerializer<T> implements ItemSerializer<T>, ItemDeserializer<T> {

  @Override
  public byte[] serialize(Item<T> item, Optional<byte[]> delim) {
    byte[] json = JSONUtil.toJSONString(item).getBytes();
    byte[] header;
    byte[] combined;
    if (delim.isPresent()) {
      header=ByteBuffer.allocate(4).putInt(json.length+delim.get().length).array();
      combined=new byte[4+json.length+delim.get().length];
    }
    else {
      header = ByteBuffer.allocate(4).putInt(json.length).array();
      combined=new byte[4+json.length];
    }
    System.arraycopy(header,0,combined,0         ,4);
    System.arraycopy(json,0,combined,4         ,json.length);
    if (delim.isPresent())
      System.arraycopy(delim.get(), 0, combined, json.length + 4, delim.get().length);
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
