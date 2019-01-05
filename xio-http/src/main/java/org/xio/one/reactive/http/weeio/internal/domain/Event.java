package org.xio.one.reactive.http.weeio.internal.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.xio.one.reactive.http.weeio.internal.api.JSONUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

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
  private HashMap<String, String> requestHeaders;
  private String requestMethod;
  private String eventType;

  public Event() {
    this.generateId();
    this.eventTimestamp = System.currentTimeMillis();
    this.eventNodeId = EventNodeID.getNodeID();
  }

  public Event(Object object) {
    this();
    Map<String, Object> beanProperties = new HashMap<>();
    Method[] methods = object.getClass().getDeclaredMethods();
    Arrays.stream(methods).forEach(m -> {
      Object value=null;
      try {
        value = m.invoke(object,null);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
      String fieldName = m.getName().replace("get", "").replace("is", "");
      fieldName = fieldName.replaceFirst(".",fieldName.substring(0,1).toLowerCase());
      beanProperties.put(fieldName, value);
    });
    this.eventValues = beanProperties;
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

  public long get_eventTimestamp() {
    return this.eventTimestamp;
  }

  public boolean isEventAlive(int ttlSeconds) {
    if (ttlSeconds > 0)
      return this.get_eventTimestamp() + (ttlSeconds * 1000) > System.currentTimeMillis();
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

  public long get_eventId() {
    return this.eventId;
  }

  public long get_eventNodeId() {
    return eventNodeId;
  }

  public String get_eventType() {
    return eventType;
  }

  public HashMap<String, String> get_httpRequestHeaders() {
    return requestHeaders;
  }

  public String get_httpRequestMethod() {
    return requestMethod;
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
      return this.get_eventId() == (((Event) event).get_eventId());
  }

  public String toJSONString() {
    return JSONUtil.toJSONString(readEventValues());
  }

  public String toFullJSONString() {
    return JSONUtil.toJSONString(this);
  }

  public void addHTTPHeaders(HashMap<String, Enumeration<String>> requestHeaders) {
    HashMap<String, String> headers = new HashMap<>();
    requestHeaders.keySet().forEach(k -> {
      ArrayList<String> values = new ArrayList<>();
      requestHeaders.get(k).asIterator().forEachRemaining(v -> values.add(v));
      headers.put(k, values.get(0));
    });
    this.requestHeaders = headers;
  }

  public void addRequestMethod(String requestMethod) {
    this.requestMethod = requestMethod;
  }

  public void addEventType(String eventType) {
    this.eventType = eventType;
  }

  public String toSSECompact() {
    return "data: " + toJSONString();
  }

  public String toSSEFull() {
    String event = "event: " + eventType;
    String data =
        "data: "+ toFullJSONString();
    return event + "\n" + data;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("Event{");
    sb.append("eventId=").append(eventId);
    sb.append(", eventTimestamp=").append(eventTimestamp);
    sb.append(", eventNodeId=").append(eventNodeId);
    sb.append(", eventValues=").append(eventValues);
    sb.append(", requestHeaders=").append(requestHeaders);
    sb.append(", requestMethod='").append(requestMethod).append('\'');
    sb.append(", eventType='").append(eventType).append('\'');
    sb.append('}');
    return sb.toString();
  }
}

