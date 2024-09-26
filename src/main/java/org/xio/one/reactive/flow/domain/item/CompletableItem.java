package org.xio.one.reactive.flow.domain.item;

import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;

public class CompletableItem<T, R> extends Item<T> {

  private FlowItemCompletionHandler<R, T> callback;

  public CompletableItem(T value, long itemId, long itemTTLSeconds) {
    super(value, itemId, itemTTLSeconds);
  }

  public CompletableItem(T value, long itemId, long itemTTLSeconds,
      FlowItemCompletionHandler<R, T> callback) {
    this(value, itemId, itemTTLSeconds);
    this.callback = callback;
  }

  public FlowItemCompletionHandler<R, T> flowItemCompletionHandler() {
    return callback;
  }


}
