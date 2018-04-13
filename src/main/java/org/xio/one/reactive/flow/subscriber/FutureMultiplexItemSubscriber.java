package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.FlowItem;

import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public abstract class FutureMultiplexItemSubscriber<R, E>
    extends FutureSubscriber<R, E> {

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  @Override
  public final void process(NavigableSet<FlowItem<E>> e) {
    Map<Long, Future<R>> streamResults;
    if (e != null) {
      try {
        streamResults = onNext(e.stream());
        e.stream().forEach(i -> completeFuture(i, streamResults.get(i.itemId())));
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }

    }
  }

  public abstract Map<Long, Future<R>> onNext(Stream<FlowItem<E>> e);

}
