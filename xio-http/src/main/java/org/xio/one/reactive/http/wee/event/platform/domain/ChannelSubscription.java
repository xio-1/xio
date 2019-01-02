package org.xio.one.reactive.http.wee.event.platform.domain;

import org.xio.one.reactive.http.wee.event.platform.domain.selector.FilterSelector;

public class ChannelSubscription {
  private long lastSeenEventId;
  private FilterSelector filterSelector;

  public ChannelSubscription(FilterSelector filterSelector) {
    this.lastSeenEventId = 0;
    this.filterSelector = filterSelector;
  }

  public long getLastSeenEventId() {
    return lastSeenEventId;
  }

  public FilterSelector getFilterSelector() {
    return filterSelector;
  }

  public void setLastSeenEventId(long lastSeenEventId) {
    this.lastSeenEventId = lastSeenEventId;
  }
}
