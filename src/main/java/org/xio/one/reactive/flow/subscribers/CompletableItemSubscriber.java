package org.xio.one.reactive.flow.subscribers;

import java.util.NavigableSet;
import org.xio.one.reactive.flow.domain.item.CompletableItem;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.CompletableSubscriber;

public abstract class CompletableItemSubscriber<R, T> extends CompletableSubscriber<R, T> {

  private final boolean parallel;

  public CompletableItemSubscriber() {
    super();
    this.parallel = false;
  }

  public CompletableItemSubscriber(boolean parallel) {
    super();
    this.parallel = parallel;
  }

  @Override
  public final void process(NavigableSet<? extends Item<T>> e) {
    if (this.parallel) {
      e.parallelStream().forEach(this::accept);
    } else {
      e.forEach(this::accept);
    }
  }

  public abstract void onNext(CompletableItem<T, R> itemValue) throws Throwable;

  public abstract void onError(Throwable error, Item<T> itemValue);

  @Override
  public void initialise() {
  }

  @Override
  public R finalise() {
    return null;
  }

  private void accept(Item<T> item) {
    try {
      if (item instanceof CompletableItem) {
        onNext((CompletableItem<T, R>) item);
      }
    } catch (Throwable e) {
      onError(e, item);
    }
  }
}
