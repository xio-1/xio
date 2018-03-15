package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class SingleSubscriber<R, E> extends AbstractSubscriber<R, E> {

  @Override
  public final void process(Stream<Event<E>> e) {
    e.forEach(this::accept);
  }

  public abstract Optional<R> onNext(E eventValue) throws Throwable;

  public abstract Optional<Object> onError(Throwable error, E eventValue);

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(Event<E> event) {
    try {
      onNext(event.value()).ifPresent(this::callCallbacks);
    } catch (Throwable e) {
      callCallbacks(e, onError(e, event.value()));
    }
  }
}
