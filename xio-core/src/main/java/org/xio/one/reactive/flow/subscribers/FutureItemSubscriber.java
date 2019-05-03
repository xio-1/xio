package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

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
  public final void process(NavigableSet<Item<T, R>> e) {
    if (e != null) {
      {
        e.parallelStream().forEach(item -> {
          try {
            while (getFutures().get(item.itemId()) == null) {
              LockSupport.parkNanos(100000);
            }
            CompletableFuture<R> future = getFutures().get(item.itemId());
            future.completeAsync(() -> {
              try {
                return onNext(item);
              } catch (Throwable t) {
                onFutureCompletionError(t, item);
              }
              return null;
            });
          } catch (Throwable ex) {
            ex.printStackTrace();
          }
        });
      }
    }
  }

  public abstract R onNext(Item<T, R> itemValue) throws RuntimeException;

  public abstract void onFutureCompletionError(Throwable error, Item<T, R> itemValue);

  @Override
  public R finalise() {
    return null;
  }

}
