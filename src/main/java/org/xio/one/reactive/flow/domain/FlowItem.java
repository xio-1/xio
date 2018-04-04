/*
 * ItemImpl.java
 *
 * Created on 01 July 2007, 11:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.xio.one.reactive.flow.domain;

import org.xio.one.reactive.flow.util.JSONUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Abstract Item to be extended by user defined Items
 *
 * @author Xio
 */
public class FlowItem<E> {
  private long itemId;
  private long itemTimestamp;
  private long itemNodeId;
  private Object indexKeyValue;
  private long itemTTLSeconds;
  protected E itemValue;

  public FlowItem() {
    this.itemNodeId = NodeID.getNodeID();
    this.itemTimestamp = System.currentTimeMillis();
    this.itemId = 0;
    this.itemTTLSeconds = 0;
  }

  public FlowItem(long itemId) {
    this.itemNodeId = NodeID.getNodeID();
    this.itemTimestamp = Long.MAX_VALUE;
    this.itemId = itemId;
    this.itemTTLSeconds = 0;
  }

  public FlowItem(E value, long itemId) {
    this.itemTimestamp = System.currentTimeMillis();
    this.indexKeyValue = value.hashCode();
    this.itemValue = value;
    this.itemNodeId = NodeID.getNodeID();
    this.itemId = itemId;
    this.itemTTLSeconds = 0;
  }

  public FlowItem(E value, long itemId, long itemTTLSeconds) {
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

  public boolean isAlive() {
    if (itemTTLSeconds > 0) {
      if (this.itemTimestamp() + (itemTTLSeconds * 1000) > System.currentTimeMillis())
        return true;
      else
        return false;
    } else
      return true;
  }

  public boolean isAlive(long lastSeenItemId) {
    return isAlive() || lastSeenItemId < this.itemId;
  }

  public long itemId() {
    return this.itemId;
  }

  public long itemNodeId() {
    return itemNodeId;
  }

  public E value() {
    return itemValue;
  }

  public Object indexKeyValue() {
    return indexKeyValue;
  }

  public String jsonValue() {
    return JSONUtil.toJSONString(itemValue);
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
    } catch (IllegalAccessException e3) {
    } catch (InvocationTargetException e4) {
    }

    return toreturn;
  }

  @Override
  public boolean equals(Object item) {
    if (this == item)
      return true;
    else
      return this.itemId() == (((FlowItem) item).itemId());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("Item{");
    sb.append("itemValue=").append(itemValue.toString());
    sb.append('}');
    return sb.toString();
  }
}
