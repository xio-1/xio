package org.xio.one.test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.xio.one.reactive.flow.domain.item.Item;

@JsonAutoDetect
public class ItemToJSONPojo<T> extends Item<T> {

  public ItemToJSONPojo() {
  }

  @JsonCreator()
  public ItemToJSONPojo(T itemValue, long itemId, long itemTTLSeconds) {
    super(itemValue, itemId, itemTTLSeconds);
  }

  @Override
  public long getItemTTLSeconds() {
    return super.getItemTTLSeconds();
  }

  @Override
  public long getItemTimestamp() {
    return super.getItemTimestamp();
  }

  @Override
  public boolean isAlive() {
    return super.isAlive();
  }

  @Override
  public boolean isReadyForHouseKeeping(long maxTTLSeconds) {
    return super.isReadyForHouseKeeping(maxTTLSeconds);
  }

  @Override
  public long getItemId() {
    return super.getItemId();
  }

  @Override
  public long getItemNodeId() {
    return super.getItemNodeId();
  }

  @Override
  public T getItemValue() {
    return super.getItemValue();
  }


  public void setItemValue(T itemValue) {
    super.setItemValue(itemValue);
  }
}


