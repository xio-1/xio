package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.stream.Stream;

public class ContinuousCountingStreamSubscriber extends BaseSubscriber<Long> {

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
