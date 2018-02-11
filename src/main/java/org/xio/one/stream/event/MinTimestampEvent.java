package org.xio.one.stream.event;

public final class MinTimestampEvent extends Event {

  public MinTimestampEvent(long timestamp) {
    super(-1, timestamp);
  }

  @Override
  public boolean isEventAlive(int ttlSeconds) {
    return false;
  }

  @Override
  public long getEventId() {
    return -1;
  }
}
