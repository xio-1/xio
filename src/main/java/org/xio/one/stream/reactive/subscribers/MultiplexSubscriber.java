package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class MultiplexSubscriber<R, E> extends AbstractSubscriber<R, E> {

  @Override
  public final void process(Stream<Event<E>> e) {
    accept(e);
  }

  public abstract Optional<Map<Long, R>> onNext(Stream<Event<E>> events) throws Throwable;

  public abstract Optional<Object> onError(Throwable error, Stream<Event<E>> eventValue);

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(Stream<Event<E>> event) {
    try {
      onNext(event).ifPresent(
          c -> c.entrySet().stream().parallel().forEach(value -> callCallbacks(value.getValue())));
    } catch (Throwable e) {
      callCallbacks(e, onError(e, event));
    }
  }
}
