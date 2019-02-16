package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.AbstractSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.functional.OnNextFunction;

import java.util.NavigableSet;

public abstract class FlowItemSubscriber<R, T> extends AbstractSubscriber<R, T>
    implements OnNextFunction<T, R> {

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
