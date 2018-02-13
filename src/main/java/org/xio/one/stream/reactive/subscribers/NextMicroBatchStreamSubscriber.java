package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.stream.Stream;

public abstract class NextMicroBatchStreamSubscriber<R> extends BaseSubscriber<R> {

  @Override
  protected R process(Stream<Event> e) {
    if (e != null) {
      return processStream(e);
    } else return null;
  }

  protected abstract R processStream(Stream<Event> e);

}
