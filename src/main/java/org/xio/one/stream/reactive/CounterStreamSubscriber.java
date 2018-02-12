package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;

import java.util.stream.Stream;

public class CounterStreamSubscriber extends BaseSubscriber<Long> {

  volatile Long count = 0L;

  @Override
  public void initialise() {
    this.result = null;
  }

  @Override
  public Long peek() {
    return count;
  }

  @Override
  protected Long process(Stream<Event> e) {
    count += e.count();
    return count;
  }

}
