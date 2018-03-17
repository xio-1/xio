package org.xio.one.reactive.flow.subscribers;

import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;
import org.xio.one.reactive.flow.events.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class MultiplexFutureSubscriber<R, E> extends AbstractFutureSubscriber<R, E> {

  BlockingMap<Long, R> results = new BlockingHashMap<>();
  Map<Long, Future<R>> futures = new HashMap<>();

  @Override
  public void initialise() {
  }

  @Override
  public void finalise() {
  }

  public final Future<R> register(long eventId) {
    Future<R> futureResult = new MultiplexFuture<>(eventId, results);
    futures.put(eventId, new MultiplexFuture<>(eventId, results));
    return futureResult;
  }

  @Override
  public final void process(Stream<Event<E>> e) {
    if (e != null) {
      Map<Long, R> streamResults = onNext(e);
      streamResults.keySet().stream().parallel()
          .forEach(eventId -> results.put(eventId, streamResults.get(eventId)));
      streamResults.entrySet().stream().parallel()
          .forEach(value -> callCallbacks(value.getValue()));
    }
  }

  public abstract Map<Long, R> onNext(Stream<Event<E>> e);

  class MultiplexFuture<R> implements Future<R> {

    long eventId;
    BlockingMap<Long, R> results;
    boolean done = false;

    public MultiplexFuture(long eventId, BlockingMap<Long, R> results) {
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
