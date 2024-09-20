package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;

public abstract class FutureItemSubscriber<R, T> extends FutureSubscriber<R, T> {

  private boolean parallel;

  public FutureItemSubscriber() {
    super();
    this.parallel = false;
  }

  public FutureItemSubscriber(boolean parallel) {
    super();
    this.parallel = parallel;
  }

  @Override
  public void initialise() {
  }

  @Override
  public final void process(NavigableSet<? extends Item<T>> e) {
    if (e != null)
      e.parallelStream().forEach(this::submitNext);

  }

  private void submitNext(Item<T> item) {
    CompletableFuture<R> future = getFutures().get(item.itemId());
    future.completeAsync(() -> {
      try {
        return onNext(item);
      } catch (Throwable t) {
        onError(t, item);
      }
      return null;
    }).thenRun(() -> deregisterCompletableFuture(item.itemId()));

  }

  public abstract R onNext(Item<T> itemValue) throws RuntimeException;

  public abstract void onError(Throwable error, Item<T> itemValue);

  @Override
  public R finalise() {
    return null;
  }



}
