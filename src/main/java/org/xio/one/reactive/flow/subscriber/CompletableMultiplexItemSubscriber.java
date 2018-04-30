package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.stream.Stream;

public abstract class CompletableMultiplexItemSubscriber<R, E> extends CompletableSubscriber<R, E> {

  @Override
  public final void process(NavigableSet<FlowItem<E>> e) {
    try {
      onNext(e.stream());
    } catch (Throwable ex) {
      onError(ex, e.iterator());
    }
  }

  public abstract void onNext(Stream<FlowItem<E>> items);

  public void onError(Throwable ex, Iterator<FlowItem<E>> flowItems) {
  }

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

}
