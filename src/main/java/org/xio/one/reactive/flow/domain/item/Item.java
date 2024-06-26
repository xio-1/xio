/*
 * ItemImpl.java
 *
 * Created on 01 July 2007, 11:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.xio.one.reactive.flow.domain.item;

import org.xio.one.reactive.flow.domain.NodeID;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Abstract Item to be extended by user defined Items
 *
 * @author Xio
 */
public class Item<T> {
  protected T itemValue;
  private long itemId;
  private long itemTimestamp;
  private long itemNodeId;
  private Object indexKeyValue;
  private long itemTTLSeconds;


  public Item() {
    this.itemNodeId = NodeID.getNodeID();
    this.itemTimestamp = System.currentTimeMillis();
    this.itemId = 0;
    this.itemTTLSeconds = 0;
  }

  public Item(long itemId) {
    this.itemNodeId = NodeID.getNodeID();
    this.itemTimestamp = Long.MAX_VALUE;
    this.itemId = itemId;
    this.itemTTLSeconds = 0;
  }

  public Item(T value, long itemId) {
    this.itemTimestamp = System.currentTimeMillis();
    this.indexKeyValue = value.hashCode();
    this.itemValue = value;
    this.itemNodeId = NodeID.getNodeID();
    this.itemId = itemId;
    this.itemTTLSeconds = 0;
  }

  public Item(T value, long itemId, long itemTTLSeconds) {
    this.itemTimestamp = System.currentTimeMillis();
    this.indexKeyValue = value.hashCode();
    this.itemValue = value;
    this.itemNodeId = NodeID.getNodeID();
    this.itemId = itemId;
    this.itemTTLSeconds = itemTTLSeconds;
  }



  public long itemTimestamp() {
    return this.itemTimestamp;
  }

  public boolean alive() {
    if (itemTTLSeconds > 0) {
      if (this.itemTimestamp() + (itemTTLSeconds * 1000) > System.currentTimeMillis())
        return true;
      else
        return false;
    } else
      return true;
  }

  public boolean readyForHouseKeeping(long maxTTLSeconds) {
    if (this.itemTimestamp() + (maxTTLSeconds * 1000) > System.currentTimeMillis())
      return true;
    else
      return false;
  }

  public boolean alive(long lastSeenItemId) {
    if (alive() || lastSeenItemId < this.itemId)
      return true;
    else
      return false;
  }

  public long itemId() {
    return this.itemId;
  }

  public long itemNodeId() {
    return itemNodeId;
  }

  public T value() {
    return itemValue;
  }

  public Object indexKeyValue() {
    return indexKeyValue;
  }

  public Object getFieldValue(String fieldname) {
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
  }

  @Override
  public boolean equals(Object item) {
    if (item == null)
      return false;
    else if (this == item)
      return true;
    else
      return this.itemId() == (((Item) item).itemId());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("{\"item\":{");
    sb.append("\"encodedItemValueAsString\":").append("\"").append( URLEncoder.encode(itemValue.toString(),
        StandardCharsets.UTF_8)).append("\"");
    sb.append(", \"itemClass\":").append("\"").append(this.itemValue.getClass().getCanonicalName()).append("\"");
    sb.append(", \"itemId\":").append(itemId);
    sb.append(", \"itemTimestamp\":").append(itemTimestamp);
    sb.append(", \"itemNodeId\":").append(itemNodeId);
    sb.append(", \"itemTTLSeconds\":").append(itemTTLSeconds);
    sb.append("}}");
    return sb.toString();
  }
}
