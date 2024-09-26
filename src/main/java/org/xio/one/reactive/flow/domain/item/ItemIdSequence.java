package org.xio.one.reactive.flow.domain.item;

import java.util.concurrent.atomic.AtomicLong;

public class ItemIdSequence {

  private final AtomicLong counter;

  public ItemIdSequence() {
    counter = new AtomicLong(0);
  }

  public ItemIdSequence(long startFrom) {
    counter = new AtomicLong(startFrom);
  }

  public long getCurrent() {
    return counter.get();
  }

  public long getNext() {
    return counter.incrementAndGet();
  }

  public void reset() {
    counter.set(0);
  }

}
