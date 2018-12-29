package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscriber.internal.SubscriberInterface;

import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;


public abstract class FutureSubscriber<R, T> implements SubscriberInterface<R, T> {

  private final ForkJoinPool pool = new ForkJoinPool(10);
  private final String id = UUID.randomUUID().toString();
  private final Object lock = new Object();
  protected int delayMS = 0;
  private volatile R result = null;
  private boolean done = false;
  private Map<Long, CompletableFuture<R>> futures = new ConcurrentHashMap<>();

  public FutureSubscriber() {
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
      this.finalise();
      R toreturn = result;
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
  public R getResult() {
    return result;
  }

  @Override
  public final void setResult(R result) {
    this.result = result;
  }

  public final Future<R> register(long itemId, CompletableFuture<R> completableFuture) {
    futures.put(itemId, completableFuture);
    return completableFuture;
  }

  private R handleResult(Future<R> result, T value) {
    try {
      return result.get();
    } catch (InterruptedException | ExecutionException e) {
      onFutureCompletionError(e, value);
    }
    return null;
  }

  final void completeFuture(Item<T, R> item, Future<R> result) {
    while (futures.get(item.itemId()) == null) {
      LockSupport.parkNanos(100000);
    }
    CompletableFuture<R> future = futures.get(item.itemId());
    future.complete(handleResult(result, item.value()));


    /*ForkJoinTask<R> forkJoinTask = new ForkJoinTask<R>() {
      R rawResult;

      @Override
      public R getRawResult() {
        return rawResult;
      }

      @Override
      protected void setRawResult(R value) {
        rawResult = value;
      }

      @Override
      protected boolean exec() {
        try {
          setRawResult(result.get());
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
        return true;
      }
    };
    future.complete(pool.invoke(forkJoinTask));*/

    /*CompletableFuture.supplyAsync(() -> {
      try {
        future.complete(result.get());
      } catch (Exception e1) {
        future.complete(null);
        onFutureCompletionError(e1, item.value());
      }
      return null;
    });*/
  }


  public abstract void onFutureCompletionError(Throwable error, T itemValue);

}
