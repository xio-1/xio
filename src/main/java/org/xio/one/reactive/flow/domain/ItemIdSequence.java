package org.xio.one.reactive.flow.domain;

import java.util.concurrent.atomic.AtomicLong;

public class ItemIdSequence {

  public ItemIdSequence() {
  }

  private AtomicLong counter = new AtomicLong(0);

  public long getNext() {
    return counter.incrementAndGet();
  }

  public void reset() {
    counter.set(0);
  }

}