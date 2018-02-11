/*
 * EventImpl.java
 *
 * Created on 01 July 2007, 11:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.xio.one.stream.event;

import org.xio.one.stream.util.JSONUtil;

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
  protected E eventValue;

  public Event() {
    this.eventNodeId = EventNodeID.getNodeID();
    this.eventTimestamp = 0;
    this.eventId = 0;
  }

  public Event(E value, long eventId) {
    this.eventTimestamp = System.currentTimeMillis();
    this.indexKeyValue = value.hashCode();
    this.eventValue = value;
    this.eventNodeId = EventNodeID.getNodeID();
    this.eventId = eventId;
  }

  public Event(long eventId, long timestamp) {
    this.eventId = eventId;
    this.eventTimestamp = timestamp;
    this.eventNodeId = EventNodeID.getNodeID();
  }

  public long getEventTimestamp() {
    return this.eventTimestamp;
  }

  public boolean isEventAlive(int ttlSeconds) {
    if (ttlSeconds > 0)
      return this.getEventTimestamp() + (ttlSeconds * 1000) > System.currentTimeMillis();
    else
      return true;
  }

  public long getEventId() {
    return this.eventId;
  }

  public long getEventNodeId() {
    return eventNodeId;
  }

  public E getEventValue() {
    return eventValue;
  }

  public Object getIndexKeyValue() {
    return indexKeyValue;
  }

  public String toJSONString() {
    return JSONUtil.toJSONString(eventValue);
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
