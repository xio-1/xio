package org.xio.one.reactive.http.wee.event.platform.domain;

import java.util.Map;

public class EventKey {

  private final Object value;
  private final String fieldName;

  public EventKey(String fieldName, Map.Entry<String, Object> entry) {
    this.fieldName = fieldName;
    this.value = entry.getValue();
  }

  public EventKey(String fieldname, Object value) {
    this.fieldName=fieldname;
    this.value=value;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    EventKey eventKey = (EventKey) o;

    if (!value.equals(eventKey.value))
      return false;
    return fieldName.equals(eventKey.fieldName);
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + fieldName.hashCode();
    return result;
  }
}
