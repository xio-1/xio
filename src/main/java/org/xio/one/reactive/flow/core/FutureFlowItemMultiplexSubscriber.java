package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.core.domain.FlowItem;

import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public abstract class FutureFlowItemMultiplexSubscriber<R, E>
    extends FutureSubscriber<R, E> {

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  @Override
  public final void process(NavigableSet<FlowItem<E>> e) {
    if (e != null) {
      Map<Long, Future<R>> streamResults = onNext(e.stream());
      e.stream().parallel().forEach(i -> completeFuture(i, streamResults.get(i.itemId())));
    }
  }

  public abstract Map<Long, Future<R>> onNext(Stream<FlowItem<E>> e);

}
