package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.stream.Stream;

public abstract class CompletableMultiplexItemSubscriber<R, T> extends CompletableSubscriber<R, T> {

  public CompletableMultiplexItemSubscriber() {
    super();
  }

  public CompletableMultiplexItemSubscriber(int delayMS) {
    this.delayMS = delayMS;
  }

  @Override
  public final void process(NavigableSet<FlowItem<T, R>> e) {
    try {
      onNext(e.stream());
    } catch (Throwable ex) {
      onError(ex, e.iterator());
    }
  }

  public abstract void onNext(Stream<FlowItem<T, R>> items);

  public void onError(Throwable ex, Iterator<FlowItem<T, R>> flowItems) {
  }

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

}
