package org.xio.one.reactive.flow.domain;

public class FlowException extends RuntimeException {
  public FlowException(String message) {
    super(message);
  }

  public FlowException() {
    super();
  }
}
