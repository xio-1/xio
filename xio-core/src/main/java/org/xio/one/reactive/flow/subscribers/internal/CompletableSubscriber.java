package org.xio.one.reactive.flow.subscribers.internal;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class CompletableSubscriber<R, T> implements Subscriber<R, T> {

  private final String id = UUID.randomUUID().toString();
  private final Object lock = new Object();
  private final CompletableFuture<R> completableFuture;
  protected int delayMS = 0;
  private volatile R result = null;
  private boolean done = false;

  public CompletableSubscriber() {
    this.completableFuture = new CompletableFuture<>();
    initialise();
  }

  @Override
  public final int delayMS() {
    return delayMS;
  }

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
      if (reset)
        this.initialise();
      return toreturn;
    }
  }

  @Override
  public final Future<R> getFutureResult() {
    return completableFuture;
  }

  @Override
  public final void exitAndReturn(R result) {
    completableFuture.complete(result);
    this.result = result;
    this.stop();
  }

  @Override
  public String getId() {
    return id;
  }


}
