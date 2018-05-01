package org.xio.one.reactive.flow.subscriber;

import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.subscriber.internal.SubscriberInterface;

import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class CompletableSubscriber<R, T> implements SubscriberInterface<R, T> {

  private final String id = UUID.randomUUID().toString();
  private final Object lock = new Object();
  private volatile R result = null;
  private boolean done = false;

  public CompletableSubscriber() {
    initialise();
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
  public final void emit(NavigableSet<FlowItem<T,R>> e) {
    synchronized (lock) {
      process(e);
      lock.notify();
    }
  }

  public abstract void process(NavigableSet<FlowItem<T,R>> e);

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
      if (reset)
        this.initialise();
      return toreturn;
    }
  }

  @Override
  public final SubscriberInterface<R, T> getSubscriber() {
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
  public final R getResult() {
    return getNext();
  }
}
