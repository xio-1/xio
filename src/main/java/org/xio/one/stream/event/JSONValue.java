package org.xio.one.stream.event;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.xio.one.stream.reactive.util.JSONUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.xio.one.stream.reactive.util.JSONUtil.fromJSONString;

public class JSONValue extends Event<Map<String, Object>> {

  private static Map<String, Object> EMPTY_FIELD_VALUES = new LinkedHashMap<>();
  private static final Object EMPTY_OBJECT = new Object();

  public JSONValue() {
    super();
  }

  public JSONValue(String jsonValue, long eventId) throws IOException {
    super(fromJSONString(jsonValue, EMPTY_FIELD_VALUES.getClass()), eventId);
  }

  public JSONValue(String jsonValue, long eventId, long ttlSeconds) throws IOException {
    super(fromJSONString(jsonValue, EMPTY_FIELD_VALUES.getClass()), eventId, ttlSeconds);
  }

  public JSONValue(Map<String, Object> values, long eventId) {
    super(values, eventId);
  }

  public JSONValue(Map<String, Object> values, long eventId, long ttlSeconds) {
    super(values, eventId, ttlSeconds);
  }

  @JsonAnySetter
  public void addFieldValue(String fieldname, Object value) {
    this.eventValue.put(fieldname, value);
  }

  @JsonAnyGetter
  public Map<String, Object> readEventValues() {
    return eventValue;
  }

  @JsonIgnore
  @Override
  public Object getFieldValue(String name) {
    return eventValue.get(name);
  }

  @JsonIgnore
  @Override
  public EventKey getIndexKeyValue() {
    if (this.eventValue != null && this.eventValue.size() > 0)
      return new EventKey(
          this.eventValue.keySet().iterator().next(), this.eventValue.entrySet().iterator().next());
    else return null;
  }

  @Override
  public String toString() {
    return JSONUtil.toJSONString(readEventValues());
  }
}
