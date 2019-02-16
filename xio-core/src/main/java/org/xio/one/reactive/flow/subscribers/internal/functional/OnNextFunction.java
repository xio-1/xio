package org.xio.one.reactive.flow.subscribers.internal.functional;

import org.xio.one.reactive.flow.domain.item.Item;

@FunctionalInterface
public interface OnNextFunction<T, R> {
  void onNext(Item<T, R> itemValue) throws Throwable;
}
