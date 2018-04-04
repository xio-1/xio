package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.subscriber.internal.Callback;
import org.xio.one.reactive.flow.subscriber.internal.SubscriberOperations;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class FutureSubscriber<R, E> implements SubscriberOperations<R, E> {


  private final String id = UUID.randomUUID().toString();
  private final Object lock = new Object();
  private volatile R result = null;
  private boolean done = false;
  private List<Callback<R>> callbacks = new ArrayList<>();
  private Map<Long, CompletableFuture<R>> futures = new HashMap<>();

  public FutureSubscriber() {
    initialise();
  }

  public FutureSubscriber(Callback<R> callback) {
    initialise();
    addCallback(callback);
  }

  public abstract void initialise();

  void addCallback(Callback<R> callback) {
    callbacks.add(callback);
  }

  void callCallbacks(R result) {
    callbacks.stream().parallel().forEach(callback -> callback.handleResult(result));
  }

  void callCallbacks(Throwable e, Object source) {
    callbacks.stream().parallel().forEach(callback -> callback.handleResult(result));
  }

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
  public final void emit(NavigableSet<FlowItem<E>> e) {
    synchronized (lock) {
      process(e);
      lock.notify();
    }
  }

  public abstract void process(NavigableSet<FlowItem<E>> e);

  @Override
  public final R peek() {
    R toreturn = result;
    return toreturn;
  }

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

  @Override
  public final SubscriberOperations<R, E> getSubscriber() {
    return this;
  }

  public final String getId() {
    return id;
  }

  @Override
  public final void setResult(R result) {
    this.result = result;
  }

  @Override
  public R getResult() {
    return result;
  }

  public final Future<R> register(long itemId, CompletableFuture<R> completableFuture) {
    futures.put(itemId, completableFuture);
    return completableFuture;
  }

  final void completeFuture(FlowItem<E> item, Future<R> result) {
    CompletableFuture<R> future = futures.get(item.itemId());
    CompletableFuture.supplyAsync(() -> {
      try {
        future.complete(result.get());
      } catch (Exception e1) {
        onFutureError(e1, item.value());
      }
      return null;
    });
  }


  public abstract void onFutureError(Throwable error, E itemValue);

}
