package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.NavigableSet;

public abstract class StreamItemSubscriber<R, T> extends Subscriber<R, T> {

  @Override
  public final void process(NavigableSet<FlowItem<T>> e) {
    e.forEach(this::accept);
  }

  public abstract void onNext(FlowItem<T> itemValue) throws Throwable;

  public void onError(Throwable error, FlowItem<T> itemValue) {
    return;
  }

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(FlowItem<T> item) {
    try {
      onNext(item);
    } catch (Throwable e) {
      onError(e, item);
    }
  }
}
