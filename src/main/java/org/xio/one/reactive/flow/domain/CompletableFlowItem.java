package org.xio.one.reactive.flow.domain;

public class CompletableFlowItem<R, T> extends FlowItem<T> {

  private ItemCompletionHandler<R, T> callback;

  public CompletableFlowItem(T value, long itemId, long itemTTLSeconds,
      ItemCompletionHandler<R, T> callback) {
    super(value, itemId, itemTTLSeconds);
    this.callback = callback;
  }

  public ItemCompletionHandler<R, T> completionHandler() {
    return callback;
  }

}
