package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.CompletableSubscriber;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.stream.Stream;

public abstract class CompletableMultiItemSubscriber<R, T> extends CompletableSubscriber<R, T> {

  public CompletableMultiItemSubscriber() {
    super();
  }

  public CompletableMultiItemSubscriber(int delayMS) {
    this.delayMS = delayMS;
  }

  @Override
  public final void process(NavigableSet<Item<T, R>> e) {
    try {
      onNext(e.stream());
    } catch (Throwable ex) {
      onError(ex, e.iterator());
    }
  }

  public abstract void onNext(Stream<Item<T, R>> items);

  public void onError(Throwable ex, Iterator<Item<T, R>> flowItems) {
  }

  @Override
  public void initialise() {
  }

  @Override
  public R finalise() {
    return null;
  }

}