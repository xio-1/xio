package org.xio.one.stream.event;

public final class MaxTimestampEvent extends Event {

  public MaxTimestampEvent(long timestamp) {
    super(-1, timestamp);
  }

  @Override
  public boolean isEventAlive(int ttlSeconds) {
    return false;
  }

  @Override
  public long getEventId() {
    return Long.MAX_VALUE;
  }
}
