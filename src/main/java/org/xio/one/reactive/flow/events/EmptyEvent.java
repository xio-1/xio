package org.xio.one.reactive.flow.events;

public final class EmptyEvent extends Event {

  public static final Event EMPTY_EVENT = new EmptyEvent();

  public EmptyEvent() {
    super();
  }

  @Override
  public boolean isAlive() {
    return false;
  }

  @Override
  public String toString() {
    return "{\"events\":\"EmptyEvent\"}";
  }
}
