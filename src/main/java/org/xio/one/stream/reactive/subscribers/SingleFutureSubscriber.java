package org.xio.one.stream.reactive.subscribers;

import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;
import org.xio.one.stream.event.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class SingleFutureSubscriber<R, E> extends AbstractFutureSubscriber<R, E> {

  BlockingMap<Long, R> results = new BlockingHashMap<>();
  Map<Long, Future<R>> futures = new HashMap<>();

  @Override
  public void initialise() {
  }

  public final Future<R> register(long eventId) {
    Future<R> futureResult = new SingleFuture<>(eventId, results);
    futures.put(eventId, new SingleFuture<>(eventId, results));
    return futureResult;
  }

  @Override
  public final void process(Stream<Event<E>> e) {
    if (e != null) {
      Map<Long, R> streamResults = new HashMap<>();
      e.parallel().forEach(event -> {
        try {
          R result = onNext(event.value());
          streamResults.put(event.eventId(), result);
          callCallbacks(result);
        } catch (Throwable ex) {
          callCallbacks(ex, onError(ex, event.value()));
        }

      });
      if (!streamResults.isEmpty())
        streamResults.keySet().stream().parallel()
            .forEach(eventId -> results.put(eventId, streamResults.get(eventId)));
    }
  }

  public abstract R onNext(E eventValue) throws Throwable;

  public abstract Object onError(Throwable error, E eventValue);

  @Override
  public void finalise() {

  }

  class SingleFuture<R> implements Future<R> {

    long eventId;
    BlockingMap<Long, R> results;
    boolean done = false;

    public SingleFuture(long eventId, BlockingMap<Long, R> results) {
      this.eventId = eventId;
      this.results = results;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return done;
    }

    @Override
    public R get() throws InterruptedException {
      return results.take(eventId);
    }

    @Override
    public R get(long timeout, TimeUnit unit) throws InterruptedException {
      return results.take(eventId, timeout, unit);
    }
  }


}
