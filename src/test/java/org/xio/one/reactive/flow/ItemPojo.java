package org.xio.one.reactive.flow;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.xio.one.reactive.flow.domain.item.Item;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemPojo<T> {

  private  T itemValue;
  private long itemId;
  private long itemTTLSeconds;
  private long itemTimestamp;

  public ItemPojo() {
  }

  public ItemPojo(T itemValue, long itemId, long itemTTLSeconds, long timestamp) {
    this.itemValue=itemValue;
    this.itemId=itemId;
    this.itemTTLSeconds=itemTTLSeconds;
    this.itemTimestamp=timestamp;
  }

  @JsonIgnore
  public Item<T> toItem() {
    return new Item<>(itemValue, itemId, itemTTLSeconds, itemTimestamp);
  }

  public T getItemValue() {
    return itemValue;
  }

  public long getItemId() {
    return itemId;
  }

  public long getItemTTLSeconds() {
    return itemTTLSeconds;
  }

  public long getItemTimestamp() {
    return itemTimestamp;
  }
}


