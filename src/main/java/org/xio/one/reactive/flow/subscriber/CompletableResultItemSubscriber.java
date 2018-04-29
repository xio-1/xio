package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.CompletableFlowItem;
import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.NavigableSet;

public abstract class CompletableResultItemSubscriber<R, T> extends CompletableSubscriber<R, T> {

  @Override
  public final void process(NavigableSet<FlowItem<T>> e) {
    e.forEach(item -> accept((CompletableFlowItem<R, T>) item));
  }

  public abstract R onNext(FlowItem<T> itemValue) throws Throwable;

  public abstract void onError(Throwable error, FlowItem<T> itemValue);

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(CompletableFlowItem<R, T> item) {
    try {
      item.callback().completed(onNext(item), item.value());
    } catch (Throwable e) {
      onError(e, item);
      item.callback().failed(e, item.value());
    }
  }
}
