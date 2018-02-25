package org.xio.one.stream.event;

public final class ComparitorEvent<T> extends Event<T> {

  public ComparitorEvent(long eventId) {
    super(eventId);
  }

  @Override
  public boolean isAlive() {
    return false;
  }
}
