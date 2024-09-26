package org.xio.one.reactive.flow.subscribers;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.stream.Stream;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.AbstractSubscriber;

public abstract class MultiItemSubscriber<R, T> extends AbstractSubscriber<R, T> {

  public MultiItemSubscriber() {
    super();
  }

  public MultiItemSubscriber(int delayMS) {
    super();
    this.delayMS = delayMS;
  }

  @Override
  public final void process(NavigableSet<? extends Item<T>> e) {
    try {
      onNext(e.stream());
    } catch (Throwable ex) {
      onError(ex, e.iterator());
    }
  }

  public abstract void onNext(Stream<? extends Item<T>> items);

  public void onError(Throwable ex, Iterator<? extends Item<T>> flowItems) {
  }

  @Override
  public void initialise() {
  }

  @Override
  public R finalise() {
    return null;
  }

}
