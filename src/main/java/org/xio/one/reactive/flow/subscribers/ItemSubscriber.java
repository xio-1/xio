package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.AbstractSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.functional.OnNextFunction;

import java.util.NavigableSet;

public abstract class ItemSubscriber<R, T> extends AbstractSubscriber<R, T>
    implements OnNextFunction<T, R> {

  public ItemSubscriber() {
    super();
  }

  public ItemSubscriber(String id) {
    super(id);
  }

  @Override
  public final void process(NavigableSet<? extends Item<T>> e) {
    e.forEach(this::accept);
  }

  @Override
  public abstract void onNext(Item<T> item);

  public void onError(Throwable error, Item<T> item) {
    return;
  }

  @Override
  public void initialise() {
  }

  @Override
  public R finalise() {
    return null;
  }

  private void accept(Item<T> item) {
    try {
      onNext(item);
    } catch (Throwable e) {
      onError(e, item);
    }
  }

}
