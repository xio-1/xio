package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class AbstractFutureSubscriber<R,E> implements Subscriber<R,E> {


  private final String id = UUID.randomUUID().toString();
  private final Object lock = new Object();
  private volatile R result = null;
  private boolean done = false;
  private List<Callback<R>> callbacks = new ArrayList<>();

  public AbstractFutureSubscriber() {
    initialise();
  }

  public AbstractFutureSubscriber(Callback<R> callback) {
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
  public final void emit(Stream<Event<E>> e) {
    synchronized (lock) {
      process(e);
      lock.notify();
    }
  }

  public abstract void process(Stream<Event<E>> e);

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
  public final Subscriber<R, E> getSubscriber() {
    return this;
  }

  public final String getId() {
    return id;
  }

  @Override
  public final void setResult(R result) {
    this.result = result;
  }
}
