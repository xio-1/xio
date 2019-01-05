package org.xio.one.reactive.http.weeio.internal.domain.request;

import org.xio.one.reactive.http.weeio.internal.domain.selector.FilterOperations;

public class PassthroughExpression extends FilterExpression {
  public PassthroughExpression() {
    super("*", FilterOperations.EQ, "*");
  }

  @Override
  public String getField() {
    return super.getField();
  }

  @Override
  public FilterOperations getOperator() {
    return super.getOperator();
  }

  @Override
  public String getValue() {
    return super.getValue();
  }
}

