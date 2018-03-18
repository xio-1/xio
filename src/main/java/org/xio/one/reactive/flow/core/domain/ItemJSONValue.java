package org.xio.one.reactive.flow.core.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.xio.one.reactive.flow.core.util.JSONUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ItemJSONValue extends Item<Map<String, Object>> {

  private static Map<String, Object> EMPTY_FIELD_VALUES = new LinkedHashMap<>();
  private static final Object EMPTY_OBJECT = new Object();

  public ItemJSONValue() {
    super();
  }

  public ItemJSONValue(String jsonValue, long itemId) throws IOException {
    super(JSONUtil.fromJSONString(jsonValue, EMPTY_FIELD_VALUES.getClass()), itemId);
  }

  public ItemJSONValue(String jsonValue, long itemId, long ttlSeconds) throws IOException {
    super(JSONUtil.fromJSONString(jsonValue, EMPTY_FIELD_VALUES.getClass()), itemId, ttlSeconds);
  }

  public ItemJSONValue(Map<String, Object> values, long itemId) {
    super(values, itemId);
  }

  public ItemJSONValue(Map<String, Object> values, long itemId, long ttlSeconds) {
    super(values, itemId, ttlSeconds);
  }

  @JsonAnySetter
  public void addFieldValue(String fieldname, Object value) {
    this.itemValue.put(fieldname, value);
  }

  @JsonAnyGetter
  public Map<String, Object> readItemValues() {
    return itemValue;
  }

  @JsonIgnore
  @Override
  public Object getFieldValue(String name) {
    return itemValue.get(name);
  }

  @JsonIgnore
  @Override
  public ItemKey indexKeyValue() {
    if (this.itemValue != null && this.itemValue.size() > 0)
      return new ItemKey(
          this.itemValue.keySet().iterator().next(), this.itemValue.entrySet().iterator().next());
    else return null;
  }

  @Override
  public String toString() {
    return JSONUtil.toJSONString(readItemValues());
  }
}
