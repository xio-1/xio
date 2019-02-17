package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.AbstractSubscriber;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.stream.Stream;

public abstract class FlowMultiplexItemSubscriber<R, T> extends AbstractSubscriber<R, T> {

  public FlowMultiplexItemSubscriber() {
    super();
  }

  public FlowMultiplexItemSubscriber(int delayMS) {
    super();
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
