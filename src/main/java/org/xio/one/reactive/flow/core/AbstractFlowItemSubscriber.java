package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.core.domain.FlowItem;

import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class AbstractFlowItemSubscriber<R, E> implements Subscriber<R, E> {

  private final String id = UUID.randomUUID().toString();
  private final Object lock = new Object();
  private volatile R result = null;
  private boolean done = false;

  public AbstractFlowItemSubscriber() {
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

  @Override
  public R getResult() {
    return getNext();
  }
}
