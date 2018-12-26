package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.NavigableSet;

public abstract class StreamItemSubscriber<R, T> extends Subscriber<R, T> {

  @Override
  public final void process(NavigableSet<Item<T, R>> e) {
    e.forEach(this::accept);
  }

  public abstract void onNext(Item<T, R> itemValue) throws Throwable;

  public void onError(Throwable error, Item<T, R> itemValue) {
    return;
  }

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(Item<T, R> item) {
    try {
      onNext(item);
    } catch (Throwable e) {
      onError(e, item);
    }
  }
}
