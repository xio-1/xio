package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public abstract class MultiplexSubscriber<R, E> extends AbstractSubscriber<Map<Long,R>, E> {
  
  @Override
  public final void process(Stream<Event<E>> e) {
    accept(e);
  }

  public abstract Map<Long,R> onNext(Stream<Event<E>> events) throws Throwable;

  public abstract Object onError(Throwable error, Stream<Event<E>> eventValue);

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(Stream<Event<E>> event) {
    try {
      callCallbacks(onNext(event));
    } catch (Throwable e) {
      callCallbacks(e, onError(e, event));
    }
  }
}
