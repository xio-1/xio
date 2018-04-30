package org.xio.one.reactive.flow.domain;

public class IOCompletionHandler<R,T> implements java.nio.channels.CompletionHandler<R,T> {

  ItemCompletionHandler<R,T> ioCompletionHandler;

  public static final <R,T> IOCompletionHandler <R,T> aIOCompletionHandler(ItemCompletionHandler<R,T> completionHandler) {
    return new IOCompletionHandler<>(completionHandler);
  }

  protected IOCompletionHandler(ItemCompletionHandler completionHandler) {
    ioCompletionHandler = completionHandler;
  }

  @Override
  public void completed(R result, T attachment) {
    ioCompletionHandler.completed(result,attachment);
  }

  @Override
  public void failed(Throwable exc, T attachment) {
    ioCompletionHandler.failed(exc,attachment);
  }
}
