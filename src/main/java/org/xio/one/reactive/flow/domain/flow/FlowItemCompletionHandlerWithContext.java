package org.xio.one.reactive.flow.domain.flow;

public class FlowItemCompletionHandlerWithContext<C> implements FlowItemCompletionHandler {

    C context;

    public FlowItemCompletionHandlerWithContext(C context) {
        this.context=context;
    }

    @Override
    public void completed(Object result, Object attachment) {

    }

    @Override
    public void failed(Throwable exc, Object attachment) {

    }

    public C getContext() {
        return context;
    }
}
