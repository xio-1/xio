package org.xio.one.reactive.http.wee.event.platform.domain.request;

import org.xio.one.reactive.http.wee.event.platform.domain.selector.FilterEntry;
import org.xio.one.reactive.http.wee.event.platform.domain.selector.FilterOperations;
import org.xio.one.reactive.http.wee.event.platform.domain.selector.FilterSelectorException;

public class FilterExpression {

  String field;
  FilterOperations operator;
  String value;

  public FilterExpression(String field, FilterOperations operator, String value) {
    this.field = field;
    this.operator = operator;
    this.value = value;
  }

  public FilterExpression() {
  }

  public FilterEntry filterEntry() {
    Object javaValue=null;
    if (validate()) {
      if (value.startsWith("'") && value.endsWith("'")) {
        if (value.equals("''"))
          javaValue = "";
        else
          javaValue = value.substring(1, value.length() - 1);
      } else {
        try {
          javaValue = Integer.parseInt(value);
        } catch (Exception e1) {
          try {
            javaValue = Long.parseLong(value);
          } catch (Exception e2) {
            try {
              javaValue = Double.parseDouble(value);
            } catch (Exception e3) {
              try {
                javaValue = Float.parseFloat(value);
              } catch (Exception e4) {
                throw new FilterSelectorException("UNKNOWN DATA TYPE");
              }
            }
          }
        }
      }
    }
    return new FilterEntry(field, operator, javaValue);
  }

  private boolean validate() {
    if (field == null || field.isEmpty() || field.isBlank())
      throw new FilterSelectorException("field must be specified");
    if (operator == null)
      throw new FilterSelectorException("operator must be specified");
    if (value == null || value.isBlank() || value.isEmpty())
      throw new FilterSelectorException("value must be specified");
    return true;
  }

  public String getField() {
    return field;
  }

  public FilterOperations getOperator() {
    return operator;
  }

  public String getValue() {
    return value;
  }
}
