package org.xio.one.reactive.flow.subscribers;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;


public abstract class FutureSubscriber<R, T> implements Subscriber<R, T> {

  private final ForkJoinPool pool = new ForkJoinPool(10);
  private final String id = UUID.randomUUID().toString();
  private final Object lock = new Object();
  private final Map<Long, CompletableFuture<R>> futures =
      Collections.synchronizedMap(new ConcurrentHashMap<>());
  private final CompletableFuture<R> completableFuture;
  protected int delayMS = 0;
  private volatile R result = null;
  private boolean done = false;
  private Map<String, Object> context;

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
  public final void emit(NavigableSet<Item<T>> e) {
    synchronized (lock) {
      process(e);
      lock.notify();
    }
  }

  public abstract void process(NavigableSet<? extends Item<T>> e);


  @Override
  public final R getNext() {
    return getWithReset(0, TimeUnit.MILLISECONDS, false);
  }

  private R getWithReset(long timeout, TimeUnit timeUnit, boolean reset) {
    synchronized (lock) {
      while (result == null && !isDone()) {
        try {
          lock.wait(timeout);
          if (timeout > 0) {
            break;
          }
        } catch (InterruptedException e) {
        }
      }
      R toreturn = this.finalise();
      //R toreturn = result;
      result = null;
      if (reset) {
        this.initialise();
      }
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

  public final void registerCompletableFuture(long itemId, CompletableFuture<R> completableFuture) {
    futures.put(itemId, completableFuture);
  }

  public final void deregisterCompletableFuture(long itemId) {
    futures.remove(itemId);
  }

  public Map<Long, CompletableFuture<R>> getFutures() {
    return futures;
  }

  public abstract void onError(Throwable error, Item<T> itemValue);

  @Override
  public Map<String, Object> getContext() {
    return Map.of();
  }

  @Override
  public void restoreContext(Map<String, Object> context) {
    this.context = context;
  }
}
