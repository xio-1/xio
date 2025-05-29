package org.xio.one.reactive.flow.domain.flow;

public class FlowAlreadyClosedException extends RuntimeException{
    public FlowAlreadyClosedException(String message) {
        super(message);
    }
}
