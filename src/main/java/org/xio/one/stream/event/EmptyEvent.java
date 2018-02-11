package org.xio.one.stream.event;

public final class EmptyEvent extends Event {

  public static final Event EMPTY_EVENT = new EmptyEvent();

  public EmptyEvent() {
    super();
  }

  @Override
  public boolean isEventAlive(int ttlSeconds) {
    return false;
  }

}
