package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.CompletableFlowItem;
import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.NavigableSet;

public abstract class CompletableResultItemSubscriber<R, T> extends CompletableSubscriber<R, T> {

  @Override
  public final void process(NavigableSet<FlowItem<T>> e) {
    e.forEach(item -> accept((CompletableFlowItem<R, T>) item));
  }

  public abstract void onNext(CompletableFlowItem<R,T> itemValue) throws Throwable;

  public abstract void onError(Throwable error, FlowItem<T> itemValue);

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(CompletableFlowItem<R, T> item) {
    try {
      onNext(item);
    } catch (Throwable e) {
      onError(e, item);
    }
  }
}
