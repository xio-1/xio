package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;

import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;


public abstract class FutureSubscriber<R, T> implements Subscriber<R, T> {

  private final ForkJoinPool pool = new ForkJoinPool(10);
  private final String id = UUID.randomUUID().toString();
  private final Object lock = new Object();
  protected int delayMS = 0;
  private volatile R result = null;
  private boolean done = false;
  private Map<Long, CompletableFuture<R>> futures = new ConcurrentHashMap<>();
  private CompletableFuture<R> completableFuture;

  public FutureSubscriber() {
    this.completableFuture = new CompletableFuture<>();
    initialise();
  }

  @Override
  public final int delayMS() {
    return delayMS;
  }

  @Override
  public abstract void initialise();

  @Override
  public final boolean isDone() {
    return this.done;
  }

  @Override
  public final boolean stop() {
    synchronized (lock) {
      done = true;
      lock.notify();
    }
    return true;
  }

  @Override
  public final void emit(NavigableSet<Item<T, R>> e) {
    synchronized (lock) {
      process(e);
      lock.notify();
    }
  }

  public abstract void process(NavigableSet<Item<T, R>> e);


  @Override
  public final R getNext() {
    return getWithReset(0, TimeUnit.MILLISECONDS, false);
  }

  private R getWithReset(long timeout, TimeUnit timeUnit, boolean reset) {
    synchronized (lock) {
      while (result == null && !isDone())
        try {
          lock.wait(timeout);
          if (timeout > 0)
            break;
        } catch (InterruptedException e) {
        }
      R toreturn = this.finalise();
      //R toreturn = result;
      result = null;
      if (reset)
        this.initialise();
      return toreturn;
    }
  }

  public final String getId() {
    return id;
  }

  @Override
  public Future<R> getFutureResult() {
    return completableFuture;
  }

  @Override
  public final void exitAndReturn(R result) {
    this.result = result;
    completableFuture.complete(result);
    this.stop();
  }

  public final Future<R> register(long itemId, CompletableFuture<R> completableFuture) {
    futures.put(itemId, completableFuture);
    return completableFuture;
  }

  public Map<Long, CompletableFuture<R>> getFutures() {
    return futures;
  }

  public abstract void onFutureCompletionError(Throwable error, Item<T,R> itemValue);

}
