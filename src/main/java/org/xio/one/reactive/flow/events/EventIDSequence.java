package org.xio.one.reactive.flow.events;

import java.util.concurrent.atomic.AtomicLong;

public class EventIDSequence {

  public EventIDSequence() {
  }

  private AtomicLong counter = new AtomicLong(0);

  public long getNext() {
    return counter.incrementAndGet();
  }

  public void reset() {
    counter.set(0);
  }

}
