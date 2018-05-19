package org.xio.one.reactive.flow.domain;

import java.util.Map;

public class ItemKey {

  private final Object value;
  private final String fieldName;

  public ItemKey(String fieldName, Map.Entry<String, Object> entry) {
    this.fieldName = fieldName;
    this.value = entry.getValue();
  }

  public ItemKey(String fieldname, Object value) {
    this.fieldName = fieldname;
    this.value = value;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ItemKey itemKey = (ItemKey) o;

    if (!value.equals(itemKey.value))
      return false;
    return fieldName.equals(itemKey.fieldName);
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + fieldName.hashCode();
    return result;
  }
}
