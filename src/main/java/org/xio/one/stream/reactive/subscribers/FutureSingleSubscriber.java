package org.xio.one.stream.reactive.subscribers;

import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;
import org.xio.one.stream.event.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class FutureSingleSubscriber<R, E> extends BaseSubscriber<R, E> {

  BlockingMap<Long, R> results ;
  Map<Long, Future<R>> futures ;

  @Override
  public void initialise() {
    results = new BlockingHashMap<>();
    futures = new HashMap<>();
  }

  public class SingleFuture<R> implements Future<R> {

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

  public Future<R> register(long eventId) {
    Future<R> futureResult = new SingleFuture<>(eventId, results);
    futures.put(eventId, new SingleFuture<>(eventId, results));
    return futureResult;
  }

  @Override
  protected void process(Stream<Event<E>> e) {
    if (e != null) {
      Map<Long, R> streamResults = new HashMap<>();
      e.parallel()
          .forEach(
              eEvent -> {
                R result = process(eEvent.value());
                streamResults.put(eEvent.eventId(), result);
                callCallbacks(result);
              });
      if (!streamResults.isEmpty())
        streamResults
            .keySet()
            .stream()
            .parallel()
            .forEach(eventId -> results.put(eventId, streamResults.get(eventId)));
    }
  }

  public abstract R process(E e);

  @Override
  public void finalise() {

  }
}
