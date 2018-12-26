package org.xio.one.reactive.http.wee.event.platform.domain.selector;

public enum FilterOperations {
  IN("IN"), GT("GT"), LT("LT'"), EQ("EQ");

  private final String symbol;

  FilterOperations(String symbol) {
    this.symbol = symbol;
  }

  public String getSymbol() {
    return this.symbol;
  }
}