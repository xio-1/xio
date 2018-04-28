package org.xio.one.reactive.flow.domain;

import java.nio.channels.CompletionHandler;

public class CompletableFlowItem<R, T> extends FlowItem<T> {

  private CompletionHandler<R, T> completionHandler;

  public CompletableFlowItem(T value, long itemId, long itemTTLSeconds,
      CompletionHandler<R, T> completionHandler) {
    super(value, itemId, itemTTLSeconds);
    this.completionHandler = completionHandler;
  }


  public CompletionHandler<R, T> completionHandler() {
    return completionHandler;
  }



}
