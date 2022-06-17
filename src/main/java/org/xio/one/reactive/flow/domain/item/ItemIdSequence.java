package org.xio.one.reactive.flow.domain.item;

import java.util.concurrent.atomic.AtomicLong;

public class ItemIdSequence {

  private AtomicLong counter = new AtomicLong(0);

  public ItemIdSequence() {
  }

  public long getNext() {
    return counter.incrementAndGet();
  }

  public void reset() {
    counter.set(0);
  }

}
