/*
 * ItemImpl.java
 *
 * Created on 01 July 2007, 11:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.xio.one.reactive.flow.domain.item;

import java.util.Objects;

/**
 * Abstract Item to be extended by user defined Items
 *
 * @author Xio
 */
public class Item<T> {

  private T itemValue;
  private long itemId;
  private long itemTimestamp;
  private long itemTTLSeconds;

  protected Item() {
    this.itemTimestamp = System.currentTimeMillis();
    this.itemId = 0;
    this.itemTTLSeconds = 0;
    this.itemValue = null;
  }

  public Item(long itemId) {
    this.itemTimestamp = Long.MAX_VALUE;
    this.itemId = itemId;
    this.itemTTLSeconds = 0;
    this.itemValue = null;
  }

  public Item(T itemValue,  long itemId) {
    this.itemTimestamp = System.currentTimeMillis();
    this.itemValue = itemValue;
    this.itemId = itemId;
    this.itemTTLSeconds = 0;
  }

  public Item(T itemValue, long itemId, long itemTTLSeconds) {
    this.itemTimestamp = System.currentTimeMillis();
    this.itemValue = itemValue;;
    this.itemId = itemId;
    this.itemTTLSeconds = itemTTLSeconds;
  }

  public Item(T itemValue, long itemId, long itemTTLSeconds, long itemTimestamp) {
    this.itemTimestamp = itemTimestamp;
    this.itemValue = itemValue;
    this.itemId = itemId;
    this.itemTTLSeconds = itemTTLSeconds;
  }

  public long getItemTTLSeconds() {
    return itemTTLSeconds;
  }

  public long getItemTimestamp() {
    return this.itemTimestamp;
  }

  public boolean isAlive() {
    if (itemTTLSeconds > 0) {
      return this.getItemTimestamp() + (itemTTLSeconds * 1000) > System.currentTimeMillis();
    } else {
      return true;
    }
  }

  public boolean isReadyForHouseKeeping(Item item, long minimumLastSeenId, long maxTTLSeconds) {
    if (item.getItemId() > minimumLastSeenId)
      return false;
    else return System.currentTimeMillis() < ((item.getItemTTLSeconds()*1000)+getItemTimestamp());
  }

  public long getItemId() {
    return this.itemId;
  }

  public T getItemValue() {
    return itemValue;
  }

  public void setItemValue(T itemValue) {
    this.itemValue = itemValue;
  }

  /**public Object getFieldValue(String fieldname) {
    Method f = null;
    Object toreturn = null;
    try {
      f = this.itemValue.getClass()
          .getMethod("get" + fieldname.substring(0, 1).toUpperCase() + fieldname.substring(1),
              null);
      toreturn = f.invoke(this.itemValue, null);
    } catch (NoSuchMethodException e2) {
      try {
        f = this.itemValue.getClass()
            .getMethod("is" + fieldname.substring(0, 1).toUpperCase() + fieldname.substring(1),
                null);
        toreturn = f.invoke(this.itemValue, null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } catch (Exception e3) {
      throw new RuntimeException(e3);
    }

    return toreturn;
  }*/
  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Item<?> item = (Item<?>) o;
    return itemId == item.itemId && itemTimestamp == item.itemTimestamp && itemTTLSeconds == item.itemTTLSeconds && Objects.equals(itemValue, item.itemValue);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(itemId);
  }
}
