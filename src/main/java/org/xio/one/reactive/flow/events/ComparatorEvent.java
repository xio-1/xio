package org.xio.one.reactive.flow.events;

public final class ComparatorEvent<T> extends Event<T> {

  public ComparatorEvent(long eventId) {
    super(eventId);
  }

  @Override
  public boolean isAlive() {
    return false;
  }
}
