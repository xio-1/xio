package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.stream.Stream;

public abstract class StreamMultiplexItemSubscriber<R, T> extends Subscriber<R, T> {

  public StreamMultiplexItemSubscriber() {
    super();
  }

  public StreamMultiplexItemSubscriber(int delayMS) {
    super();
    this.delayMS = delayMS;
  }

  @Override
  public final void process(NavigableSet<Item<T, R>> e) {
    try {
      onNext(e.stream());
    } catch (Throwable ex) {
      onError(ex, e.iterator());
    }
  }

  public abstract void onNext(Stream<Item<T, R>> items);

  public void onError(Throwable ex, Iterator<Item<T, R>> flowItems) {
  }

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

}
