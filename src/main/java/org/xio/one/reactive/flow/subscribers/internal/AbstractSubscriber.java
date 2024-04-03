package org.xio.one.reactive.flow.subscribers.internal;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Subscriber
 *
 * @Author Richard Durley
 * @OringinalWork XIO
 * @Copyright Richard Durley / XIO.ONE
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 */
public abstract class AbstractSubscriber<R, T> implements Subscriber<R, T> {

  private final String id = UUID.randomUUID().toString();
  private final Object lock = new Object();
  protected int delayMS = 0;
  CompletableFuture<R> completableFuture;
  private volatile R result = null;
  private boolean done = false;

  public AbstractSubscriber() {
    this.completableFuture = new CompletableFuture<>();
  }

  @Override
  public abstract void initialise();

  @Override
  public R finalise() {
    return null;
  }

  @Override
  public final boolean isDone() {
    return this.done;
  }

  @Override
  public final int delayMS() {
    return delayMS;
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
      while (result == null && !isDone())
        try {
          lock.wait(timeout);
          if (timeout > 0)
            break;
        } catch (InterruptedException e) {
        }

      R toreturn = result;
      if (reset) {
        this.finalise();
        this.initialise();
      }
      return toreturn;
    }
  }

  @Override
  public final String getId() {
    return id;
  }

  @Override
  public final Future<R> getFutureResult() {
    return completableFuture;
  }

  @Override
  public final void exitAndReturn(R result) {
    this.result = result;
    completableFuture.complete(result);
    this.stop();
  }


}
