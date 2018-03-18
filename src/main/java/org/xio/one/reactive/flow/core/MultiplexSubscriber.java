package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.domain.Item;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class MultiplexSubscriber<R, E> extends AbstractSubscriber<R, E> {

  @Override
  public final void process(Stream<Item<E>> e) {
    accept(e);
  }

  public abstract Optional<Map<Long, R>> onNext(Stream<Item<E>> items) throws Throwable;

  public abstract Optional<Object> onError(Throwable error, Stream<Item<E>> itemValue);

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  private void accept(Stream<Item<E>> item) {
    try {
      onNext(item).ifPresent(
          c -> c.entrySet().stream().parallel().forEach(value -> callCallbacks(value.getValue())));
    } catch (Throwable e) {
      callCallbacks(e, onError(e, item));
    }
  }
}
