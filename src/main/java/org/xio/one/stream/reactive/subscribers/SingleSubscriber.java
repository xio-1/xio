package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.stream.Stream;

public abstract class SingleSubscriber<R, E> extends AbstractSubscriber<R, E> {

  @Override
  public final void process(Stream<Event<E>> e) {
    e.forEach(this::accept);
  }

  public abstract R onNext(E eventValue) throws Throwable;

  public abstract Object onError(Throwable error, E eventValue);

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(Event<E> event) {
    try {
      callCallbacks(onNext(event.value()));
    } catch (Throwable e) {
      callCallbacks(e, onError(e, event.value()));
    }
  }
}
