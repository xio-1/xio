package org.xio.one.reactive.http.weeio.event.platform.domain;

import java.util.concurrent.atomic.AtomicLong;

public class EventIDSequence {

  public static EventIDSequence INSTANCE = new EventIDSequence();

  private AtomicLong counter = new AtomicLong(0);

  public long getNext() {
    return counter.incrementAndGet();
  }

  public void reset() {
    counter.set(0);
  }

  ;

}
