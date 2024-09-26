package org.xio.one.reactive.flow.subscribers.internal.functional;

import org.xio.one.reactive.flow.domain.item.Item;


@FunctionalInterface
public interface OnNextReturnFunction<T, R> {

  R onNext(Item<T> itemValue) throws RuntimeException;
}
