package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.NavigableSet;
import java.util.concurrent.Future;

public abstract class FutureItemSubscriber<R, T> extends FutureSubscriber<R, T> {

  @Override
  public void initialise() {
  }

  @Override
  public final void process(NavigableSet<FlowItem<T,R>> e) {
    if (e != null) {
      e.stream().parallel().forEach(item -> {
        Future<R> result = null;
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
