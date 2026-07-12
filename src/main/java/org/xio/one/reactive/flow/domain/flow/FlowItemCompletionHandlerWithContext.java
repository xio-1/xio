package org.xio.one.reactive.flow.domain.flow;

public class FlowItemCompletionHandlerWithContext<R, A, C> implements FlowItemCompletionHandler<R,A> {

    C context;

    public FlowItemCompletionHandlerWithContext(C context) {
        this.context=context;
    }

    public C getContext() {
        return context;
    }

    @Override
    public void completed(R result, A attachment) {

    }

    @Override
    public void failed(Throwable exc, A attachment) {

    }
}
