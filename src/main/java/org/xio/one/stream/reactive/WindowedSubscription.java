package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;
import org.xio.one.stream.AsyncStream;
import org.xio.one.stream.reactive.subscribers.Subscriber;

import java.util.NavigableSet;
import java.util.concurrent.TimeUnit;

public class WindowedSubscription<E> extends Subscription<E> {

  private static final long MAX_LATENCY = 1000;
  private static final long MIN_LATENCY = 500;
  private final TimeUnit timeUnit;
  private long windowTime;
  private long fromWindowTime;
  private long toWindowTime;
  private long maxLatencyMS;

  public WindowedSubscription(AsyncStream eventStream, long windowTime, TimeUnit timeUnit,Subscriber<E> subscriber) {
    super(eventStream,subscriber);
    this.windowTime = windowTime;
    this.timeUnit = timeUnit;
    this.fromWindowTime = System.currentTimeMillis();
    this.maxLatencyMS = MAX_LATENCY;
    calcToWindowTime();
  }

  public void unsafeSetMaxLatencyMS(long maxLatencyMS) {
    this.maxLatencyMS = maxLatencyMS;
  }

  @Override
  protected NavigableSet<Event> streamContents() {
    applyThrottle();
    if (isTimeToGetNextWindow()) {
      NavigableSet<Event> e =
          getEventStream().contents().getTimeWindowSet(fromWindowTime, toWindowTime);
      calcFromWindowTime();
      calcToWindowTime();
      return e;
    } else {
      return getEventStream().contents().EMPTY_EVENT_SET;
    }
  }

  private void applyThrottle() {
    if (System.currentTimeMillis() > toWindowTime + maxLatencyMS * 2)
      getEventStream().slowdown();
    else
      getEventStream().speedup();
  }

  private boolean isTimeToGetNextWindow() {
    return System.currentTimeMillis() > toWindowTime + maxLatencyMS || (
        getEventStream().contents().getLast().getEventTimestamp() > toWindowTime + MIN_LATENCY);
  }


  private void calcToWindowTime() {
    this.toWindowTime = TimeUnit.MILLISECONDS.convert(windowTime, timeUnit) + fromWindowTime;
  }

  private void calcFromWindowTime() {
    fromWindowTime = toWindowTime + 1;
  }
}
