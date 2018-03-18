package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.domain.Item;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class SingleSubscriber<R, E> extends AbstractSubscriber<R, E> {

  @Override
  public final void process(Stream<Item<E>> e) {
    e.forEach(this::accept);
  }

  public abstract Optional<R> onNext(E itemValue) throws Throwable;

  public abstract Optional<Object> onError(Throwable error, E itemValue);

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(Item<E> item) {
    try {
      onNext(item.value()).ifPresent(this::callCallbacks);
    } catch (Throwable e) {
      callCallbacks(e, onError(e, item.value()));
    }
  }
}
