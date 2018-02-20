package org.xio.one.stream.reactive.subscribers;

import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;
import org.xio.one.stream.event.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public abstract class MicroBatchStreamSubscriber<R, E>
    extends ContinuousStreamSubscriber<Map<Long, R>, E> {

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
    public R get() throws InterruptedException, ExecutionException {
      return results.take(eventId);
    }

    @Override
    public R get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return results.take(eventId, timeout, unit);
    }
  }

  public Future<R> register(long eventId) {
    Future<R> futureResult = new BatchFuture<>(eventId, results);
    futures.put(eventId, new BatchFuture<>(eventId, results));
    return futureResult;
  }

  @Override
  protected Map<Long, R> process(Stream<Event<E>> e) {
    if (e != null) {
      Map<Long, R> streamResults = processStream(e);
      ;
      streamResults
          .keySet()
          .stream()
          .forEach(eventId -> results.put(eventId, streamResults.get(eventId)));
      return streamResults;
    } else return null;
  }

  public abstract Map<Long, R> processStream(Stream<Event<E>> e);
}
