package org.xio.one.reactive.http.weeio.event.platform.domain;

public final class EmptyEvent extends Event {

  public static final Event EMPTY_EVENT = new EmptyEvent(-1,-1);

  public EmptyEvent(long eventid, long eventTimestamp) {
    super(eventid, eventTimestamp);
  }

  @Override
  public boolean isEventAlive(int ttlSeconds) {
    return false;
  }

}
