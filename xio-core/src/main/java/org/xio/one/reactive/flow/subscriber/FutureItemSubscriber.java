package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.NavigableSet;
import java.util.concurrent.Future;

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
      if (parallel) {
        e.parallelStream().forEach(item -> {
          Future<R> result;
          try {
            result = onNext(item.value());
            completeFuture(item, result);
          } catch (Throwable ex) {
            ex.printStackTrace();
          }
        });
      } else
        e.stream().forEach(item -> {
          Future<R> result;
          try {
            result = onNext(item.value());
            completeFuture(item, result);
          } catch (Throwable ex) {
            ex.printStackTrace();
          }

        });
    }
  }

  public abstract Future<R> onNext(T itemValue) throws Throwable;

  public abstract void onFutureCompletionError(Throwable error, T itemValue);

  @Override
  public void finalise() {

  }

}
