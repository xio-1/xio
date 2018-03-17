package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.events.Event;
import org.xio.one.reactive.flow.subscribers.Subscriber;

import java.util.NavigableSet;
import java.util.concurrent.TimeUnit;

public class WindowedSubscription<R,E> extends Subscription<R,E> {

  private static final long MAX_LATENCY = 1000;
  private static final long MIN_LATENCY = 500;
  private final TimeUnit timeUnit;
  private long windowTime;
  private long fromWindowTime;
  private long toWindowTime;
  private long maxLatencyMS;

  public WindowedSubscription(AsyncFlow eventStream, long windowTime, TimeUnit timeUnit,Subscriber<R,E> subscriber) {
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
  protected NavigableSet<Event<E>> streamContents() {
    applyThrottle();
    if (isTimeToGetNextWindow()) {
      NavigableSet<Event<E>> e =
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
        getEventStream().contents().last().eventTimestamp() > toWindowTime + MIN_LATENCY);
  }


  private void calcToWindowTime() {
    this.toWindowTime = TimeUnit.MILLISECONDS.convert(windowTime, timeUnit) + fromWindowTime;
  }

  private void calcFromWindowTime() {
    fromWindowTime = toWindowTime + 1;
  }
}
