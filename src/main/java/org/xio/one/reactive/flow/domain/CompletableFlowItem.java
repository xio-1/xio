package org.xio.one.reactive.flow.domain;

public class CompletableFlowItem<R, T> extends FlowItem<T> {

  private CompletionHandler<R, T> callback;

  public CompletableFlowItem(T value, long itemId, long itemTTLSeconds,
      CompletionHandler<R, T> callback) {
    super(value, itemId, itemTTLSeconds);
    this.callback = callback;
  }

  public CompletionHandler<R, T> callback() {
    return callback;
  }

}
