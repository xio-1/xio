package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.ArrayList;
import java.util.stream.Stream;

public abstract class MicroBatchEventProcessor<R> extends BaseProcessor<R> {

  @Override
  protected R process(Stream<Event> e) {
    if (e != null) {
      this.stop();
      processStream(e);
      this.initialise();
      return process(e);
    } else return null;
  }

  protected abstract R processStream(Stream<Event> e);

}
