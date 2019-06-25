package org.xio.one.reactive.flow.domain.flow;

public class IOCompletionHandler<R, T> implements java.nio.channels.CompletionHandler<R, T> {

  FlowItemCompletionHandler<R, T> ioCompletionHandler;

  private IOCompletionHandler(FlowItemCompletionHandler<R, T> completionHandler) {
    ioCompletionHandler = completionHandler;
  }

  public static final <R, T> IOCompletionHandler<R, T> aIOCompletionHandler(
      FlowItemCompletionHandler<R, T> completionHandler) {
    return new IOCompletionHandler<>(completionHandler);
  }

  @Override
  public void completed(R result, T attachment) {
    ioCompletionHandler.completed(result, attachment);
  }

  @Override
  public void failed(Throwable exc, T attachment) {
    ioCompletionHandler.failed(exc, attachment);
  }
}
