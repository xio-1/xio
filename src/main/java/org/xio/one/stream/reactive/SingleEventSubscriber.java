package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class SingleEventSubscriber<E> extends BaseSubscriber<E> {

  private long eventId;

  public void initialise(long eventId) {
    this.eventId = eventId;
  }

  @Override
  protected E process(Stream<Event> e) {
    Optional<Event> et = e.filter(event -> event.getEventId() == eventId).limit(1).findFirst();
    if (et.isPresent()) {
      this.stop();
      return process((E) et.get().getEventValue());
    }
    else return null;
  }

  public abstract E process(E eventValue);
}
