package org.xio.one.reactive.http.wee.event.platform.domain.selector;

public class FilterEntry {

  private final String field;
  private final FilterOperations operator;
  private final Object value;

  public FilterEntry() {
    super();
    field = null;
    operator = null;
    value = null;
  }

  public FilterEntry(String field, FilterOperations operator, Object value) {
    this.field = field;
    this.operator = operator;
    this.value = value;
  }

  public String getField() {
    return field;
  }

  public FilterOperations getOperator() {
    return operator;
  }

  public Object getValue() {
    return value;
  }

  public void validate() throws SelectorException {

    if (operator == null)
      throw new SelectorException("Filter operator cannot be null");

    if (field == null || field.isEmpty())
      throw new SelectorException("Field cannot be null or empty");

    if (value == null)
      throw new SelectorException("Value cannot be null or empty");

  }
}
