/*
 * EventImpl.java
 *
 * Created on 01 July 2007, 11:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.xio.one.stream.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.xio.one.stream.reactive.util.JSONUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Abstract Event to be extended by user defined Events
 *
 * @author Xio
 */
public class Event<E> {
  private long eventId;
  private long eventTimestamp;
  private long eventNodeId;
  private Object indexKeyValue;
  private long eventTTLSeconds;
  protected E eventValue;

  public Event() {
    this.eventNodeId = EventNodeID.getNodeID();
    this.eventTimestamp = System.currentTimeMillis();
    this.eventId = 0;
    this.eventTTLSeconds = Long.MAX_VALUE;
  }

  public Event(long eventId) {
    this.eventNodeId = EventNodeID.getNodeID();
    this.eventTimestamp = Long.MAX_VALUE;
    this.eventId = eventId;
    this.eventTTLSeconds = Long.MAX_VALUE;
  }

  public Event(E value, long eventId) {
    this.eventTimestamp = System.currentTimeMillis();
    this.indexKeyValue = value.hashCode();
    this.eventValue = value;
    this.eventNodeId = EventNodeID.getNodeID();
    this.eventId = eventId;
    this.eventTTLSeconds = Long.MAX_VALUE;
  }

  public Event(E value, long eventId, long eventTTLSeconds) {
    this.eventTimestamp = System.currentTimeMillis();
    this.indexKeyValue = value.hashCode();
    this.eventValue = value;
    this.eventNodeId = EventNodeID.getNodeID();
    this.eventId = eventId;
    this.eventTTLSeconds = eventTTLSeconds;
  }

  public long getEventTimestamp() {
    return this.eventTimestamp;
  }

  public boolean isEventAlive() {
      return this.getEventTimestamp() + (eventTTLSeconds * 1000) > System.currentTimeMillis();
    }

  public long getEventId() {
    return this.eventId;
  }

  public long getEventNodeId() {
    return eventNodeId;
  }

  public E value() {
    return eventValue;
  }

  public Object getIndexKeyValue() {
    return indexKeyValue;
  }

  public String jsonValue() {
    return JSONUtil.toJSONString(eventValue);
  }

  @JsonIgnore
  public Object getFieldValue(String fieldname) {
    Method f = null;
    Object toreturn = null;
    try {
      f = this.eventValue.getClass().getMethod("get" + fieldname.substring(0,1).toUpperCase()+fieldname.substring(1), null);
      toreturn = f.invoke(this.eventValue, null);
    } catch (NoSuchMethodException e2) {
    } catch (IllegalAccessException e3) {
    } catch (InvocationTargetException e4) {
    }

    return toreturn;
  }

  @Override
  public boolean equals(Object event) {
    if (this == event)
      return true;
    else
      return this.getEventId() == (((Event) event).getEventId());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("Event{");
    sb.append("eventValue=").append(eventValue.toString());
    sb.append('}');
    return sb.toString();
  }

}
