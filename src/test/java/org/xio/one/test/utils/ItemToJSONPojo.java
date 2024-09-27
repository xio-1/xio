package org.xio.one.test.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.xio.one.reactive.flow.domain.item.Item;

public class ItemToJSONPojo<T> extends Item<T> {

  public ItemToJSONPojo() {
  }

  public ItemToJSONPojo(T value, long itemId) {
    super(value, itemId);
  }

  @Override
  public T getItemValue() {
    return super.getItemValue();
  }

  @Override
  public long getItemNodeId() {
    return super.getItemNodeId();
  }

  @Override
  public long getItemId() {
    return super.getItemId();
  }

  @Override
  public long getItemTimestamp() {
    return super.getItemTimestamp();
  }

  @Override
  public long getItemTTLSeconds() {
    return super.getItemTTLSeconds();
  }

  @Override
  public boolean isReadyForHouseKeeping(long maxTTLSeconds) {
    return super.isReadyForHouseKeeping(maxTTLSeconds);
  }

  @Override
  //@JsonIgnore
  public boolean isAlive() {
    return super.isAlive();
  }
}
