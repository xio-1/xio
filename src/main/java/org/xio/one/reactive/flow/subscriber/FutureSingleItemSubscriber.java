package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.NavigableSet;
import java.util.concurrent.Future;

public abstract class SingleFutureItemSubscriber<R, E> extends FutureSubscriber<R, E> {

  @Override
  public void initialise() {
  }

  @Override
  public final void process(NavigableSet<FlowItem<E>> e) {
    if (e != null) {
      e.stream().parallel().forEach(item -> {
        try {
          Future<R> result = onNext(item.value());
          completeFuture(item, result);
        } catch (Throwable ex) {
          onFutureError(ex, item.value());
        }
      });
    }
  }

  public abstract Future<R> onNext(E itemValue) throws Throwable;

  public abstract void onFutureError(Throwable error, E itemValue);

  @Override
  public void finalise() {

  }

}
