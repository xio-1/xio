package org.xio.one.stream.selector;

public class SelectorException extends RuntimeException {

  public SelectorException(String message) {
    super(message);
  }

  public SelectorException(String message, Throwable exception) {
    super(message, exception);
  }
}
