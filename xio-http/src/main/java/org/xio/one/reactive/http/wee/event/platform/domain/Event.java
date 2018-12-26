package org.xio.one.reactive.http.wee.event.platform.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.xio.one.reactive.http.wee.event.platform.api.JSONUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract Event to be extended by user defined Events
 *
 * @author Richard Durley
 */
public class Event {

  private static final Map<String, Object> EMPTY_FIELD_VALUES = new LinkedHashMap<>();
  private static final Object EMPTY_OBJECT = new Object();
  private long eventId = -1;
  private long eventTimestamp = -1;
  private long eventNodeId;
  private Map<String, Object> eventValues = EMPTY_FIELD_VALUES;

  public Event() {
    this.generateId();
    this.eventTimestamp = System.currentTimeMillis();
    this.eventNodeId = EventNodeID.getNodeID();
  }

  public Event(Map<String, Object> eventValues) {
    this();
    this.eventValues = eventValues;
  }

  protected Event(long eventid, long eventTimestamp) {
    this.eventId = eventid;
    this.eventTimestamp = eventTimestamp;
    this.eventNodeId = EventNodeID.getNodeID();
  }

  protected Event(long eventid, long eventTimestamp, Map<String, Object> eventValues) {
    this.eventId = eventid;
    this.eventTimestamp = eventTimestamp;
    this.eventValues = eventValues;
    this.eventNodeId = EventNodeID.getNodeID();
  }

  @JsonIgnore
  private void generateId() {
    this.eventId = EventIDSequence.INSTANCE.getNext();
  }

  @JsonIgnore
  public Object getFieldValue(String name) {
    Object toReturn;
    if (this.eventValues != null) {
      toReturn = eventValues.get(name);
    } else {
      toReturn = getFieldValueViaGetter(name);
    }
    if (toReturn != null)
      return toReturn;
    else
      return EMPTY_OBJECT;
  }

  @JsonIgnore
  public boolean isIndexFieldName(String fieldname) {
    if (this.eventValues != null && this.eventValues.size() > 0)
      return this.eventValues.keySet().iterator().next().equals(fieldname);
    else
      return false;
  }

  @JsonIgnore
  public EventKey getEventKey() {
    if (this.eventValues != null && this.eventValues.size() > 0)
      return new EventKey(this.eventValues.keySet().iterator().next(),
          this.eventValues.entrySet().iterator().next());
    else
      return null;
  }

  @JsonIgnore
  public Object getFieldValueViaColumnIndex(int columnIndex) {
    if (this.eventValues != null && this.eventValues.size() > 0)
      return this.eventValues.entrySet().iterator().next();
    else
      return EMPTY_OBJECT;
  }

  @JsonIgnore
  private Object getFieldValueViaGetter(String fieldname) {
    Method f = null;
    Object toreturn = null;
    try {
      f = this.getClass().getMethod("get" + fieldname, null);
      toreturn = f.invoke(this, null);
    } catch (NoSuchMethodException e2) {
    } catch (IllegalAccessException e3) {
    } catch (InvocationTargetException e4) {
    }

    return toreturn;
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

  @JsonIgnore
  public boolean hasFieldValue(String fieldname) {
    if (this.eventValues != null && this.eventValues.containsKey(fieldname))
      return true;
    else if (this.getFieldValueViaGetter(fieldname) != null)
      return true;
    else
      return false;
  }

  public long getEventId() {
    return this.eventId;
  }

  public long getEventNodeId() {
    return eventNodeId;
  }

  @JsonAnySetter
  public void addFieldValue(String fieldname, Object value) {
    if (eventValues.equals(EMPTY_FIELD_VALUES))
      this.eventValues = new LinkedHashMap<>();
    this.eventValues.put(fieldname, value);
  }

  @JsonAnyGetter
  public Map<String, Object> readEventValues() {
    return eventValues;
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
    return JSONUtil.toJSONString(readEventValues());
  }
}

