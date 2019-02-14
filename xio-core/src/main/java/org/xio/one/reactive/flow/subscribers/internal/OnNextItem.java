package org.xio.one.reactive.flow.subscribers.internal;

import org.xio.one.reactive.flow.domain.item.Item;

@FunctionalInterface
public interface OnNextItem<T,R> {
    void onNext(Item<T, R> itemValue) throws Throwable;
}
