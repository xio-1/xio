package org.xio.one.stream.reactive.subscribers;

import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;
import org.xio.one.stream.event.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class OnEventsSubscriber<R, E>
    extends AbstractSubscriber<Map<Long, R>, E> {

  BlockingMap<Long, R> results = new BlockingHashMap<>();
  Map<Long, Future<R>> futures = new HashMap<>();

  public class BatchFuture<R> implements Future<R> {

    long eventId;
    BlockingMap<Long, R> results;
    boolean done = false;

    public BatchFuture(long eventId, BlockingMap<Long, R> results) {
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
    Future<R> futureResult = new BatchFuture<>(eventId, results);
    futures.put(eventId, new BatchFuture<>(eventId, results));
    return futureResult;
  }

  @Override
  protected void process(Stream<Event<E>> e) {
    if (e != null) {
      Map<Long, R> streamResults = onEvents(e);
      streamResults
          .keySet()
          .stream()
          .forEach(eventId -> results.put(eventId, streamResults.get(eventId)));
      callCallbacks(streamResults);
    }
  }

  public abstract Map<Long, R> onEvents(Stream<Event<E>> e);

  @Override
  public void initialise() {

  }

  @Override
  public void finalise() {

  }
}
