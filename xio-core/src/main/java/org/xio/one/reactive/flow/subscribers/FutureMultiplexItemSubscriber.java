package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public abstract class FutureMultiplexItemSubscriber<R, T> extends FutureSubscriber<R, T> {

  public FutureMultiplexItemSubscriber() {
    super();
  }

  public FutureMultiplexItemSubscriber(int delayMS) {
    super();
    this.delayMS = delayMS;
  }

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  @Override
  public final void process(NavigableSet<Item<T, R>> e) {
    Map<Long, Future<R>> streamResults;
    if (e != null) {
      try {
        streamResults = onNext(e.stream());
        e.parallelStream().forEach(i -> completeFuture(i, streamResults.get(i.itemId())));
      } catch (Exception ex) {
        ex.printStackTrace();
      }

    }
  }

  public abstract Map<Long, Future<R>> onNext(Stream<Item<T, R>> e);

}
