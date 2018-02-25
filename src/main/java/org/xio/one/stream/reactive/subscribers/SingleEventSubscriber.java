package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class SingleEventSubscriber<R,E> extends BaseSubscriber<R,E> {

  private long eventId;

  @Override
  public void initialise() {

  }
  public void initialise(long eventId) {
    this.eventId = eventId;
  }

  @Override
  protected R process(Stream<Event<E>> e) {
    Optional<Event<E>> et = e.filter(event -> event.getEventId() == eventId).limit(1).findFirst();
    if (et.isPresent()) {
      this.stop();
      return process(et.get().value());
    }
    else return null;
  }

  public abstract R process(E eventValue);
}
