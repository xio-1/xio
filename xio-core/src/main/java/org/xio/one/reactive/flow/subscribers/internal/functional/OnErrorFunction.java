package org.xio.one.reactive.flow.subscribers.internal.functional;

import org.xio.one.reactive.flow.domain.item.Item;

@FunctionalInterface
public interface OnErrorFunction<T, R> {
  void onError(Throwable e, Item<T, R> itemValue);
}
