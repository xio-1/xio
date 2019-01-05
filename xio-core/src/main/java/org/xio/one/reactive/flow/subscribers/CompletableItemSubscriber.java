package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.NavigableSet;

public abstract class CompletableItemSubscriber<R, T> extends CompletableSubscriber<R, T> {

  private boolean parallel;

  public CompletableItemSubscriber() {
    super();
    this.parallel = false;
  }

  public CompletableItemSubscriber(boolean parallel) {
    super();
    this.parallel = parallel;
  }

  @Override
  public final void process(NavigableSet<Item<T, R>> e) {
    if (this.parallel)
      e.parallelStream().forEach(this::accept);
    else
      e.forEach(this::accept);
  }

  public abstract void onNext(Item<T, R> itemValue) throws Throwable;

  public abstract void onError(Throwable error, Item<T, R> itemValue);

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
