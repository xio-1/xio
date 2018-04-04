package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.core.domain.FlowItem;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.stream.Stream;

public abstract class FlowItemMultiplexSubscriber<R, E> extends AbstractFlowItemSubscriber<R, E> {

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
