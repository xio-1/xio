package org.xio.one.stream.event;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.xio.one.stream.util.JSONUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.xio.one.stream.util.JSONUtil.fromJSONString;

public class JSONValue extends Event<Map<String, Object>> {

  private static Map<String, Object> EMPTY_FIELD_VALUES = new LinkedHashMap<>();
  private static final Object EMPTY_OBJECT = new Object();

  public JSONValue() {
    super();
  }

  public JSONValue(String jsonValue, long eventId) throws IOException {
    super(fromJSONString(jsonValue, EMPTY_FIELD_VALUES.getClass()),eventId);
  }

  public JSONValue(Map<String, Object> values, long eventId) {
    super(values,eventId);
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
  public Object getFieldValue(String name) {
    Object toReturn;
    if (this.eventValue != null) {
      toReturn = eventValue.get(name);
    } else {
      toReturn = getFieldValueViaGetter(name);
    }
    if (toReturn != null)
      return toReturn;
    else
      return null;
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

  @JsonIgnore
  @Override
  public EventKey getIndexKeyValue() {
    if (this.eventValue != null && this.eventValue.size() > 0)
      return new EventKey(this.eventValue.keySet().iterator().next(),
          this.eventValue.entrySet().iterator().next());
    else
      return null;
  }

  @JsonIgnore
  public boolean hasFieldValue(String fieldname) {
    if (this.eventValue != null && this.eventValue.containsKey(fieldname))
      return true;
    else if (this.getFieldValueViaGetter(fieldname) != null)
      return true;
    else
      return false;
  }

  @Override
  public String toString() {
    return JSONUtil.toJSONString(readEventValues());
  }

}
