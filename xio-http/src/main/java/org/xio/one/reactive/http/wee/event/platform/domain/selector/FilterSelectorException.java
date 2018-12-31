package org.xio.one.reactive.http.wee.event.platform.domain.selector;

public class FilterSelectorException extends RuntimeException {

  public FilterSelectorException(String message) {
    super(message);
  }

  public FilterSelectorException(String message, Throwable exception) {
    super(message, exception);
  }
}
