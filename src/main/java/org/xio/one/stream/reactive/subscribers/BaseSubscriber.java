package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;
import org.xio.one.stream.reactive.subscribers.Subscriber;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class BaseSubscriber<R,E> implements Subscriber<R,E> {

  private final Object lock = new Object();
  private final String id = UUID.randomUUID().toString();
  volatile R result;
  boolean done = false;

  public BaseSubscriber() {
    initialise();
  }

  public abstract void initialise();

  @Override
  public boolean isDone() {
    return this.done;
  }

  @Override
  public boolean stop() {
    synchronized (lock) {
      done = true;
      lock.notify();
    }
    return true;
  }

  @Override
  public void emit(Stream<Event<E>> e) {
    synchronized (lock) {
      result = process(e);
      lock.notify();
    }
  }

  protected abstract R process(Stream<Event<E>> e);

  @Override
  public R peek() {
    R toreturn = result;
    return toreturn;
  }

  @Override
  public R getNext() {
    return getWithReset(0, TimeUnit.MILLISECONDS, false);
  }

  private R getWithReset(long timeout, TimeUnit timeUnit, boolean reset) {
    synchronized (lock) {
      while (result == null && !isDone())
        try {
          lock.wait(timeout);
          if (timeout > 0) break;
        } catch (InterruptedException e) {
        }
      R toreturn = result;
      result = null;
      if (reset) this.initialise();
      return toreturn;
    }
  }

  @Override
  public Subscriber<R,E> getSubscriber() {
    return this;
  }

  public String getId() {
    return id;
  }
}
