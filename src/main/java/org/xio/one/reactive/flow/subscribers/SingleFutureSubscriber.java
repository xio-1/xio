package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.events.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public abstract class SingleFutureSubscriber<R, E> extends AbstractFutureSubscriber<R, E> {

  Map<Long, CompletableFuture<R>> futures = new HashMap<>();

  @Override
  public void initialise() {
  }

  public final Future<R> register(long eventId, CompletableFuture<R> completableFuture) {
    futures.put(eventId, completableFuture);
    return completableFuture;
  }

  @Override
  public final void process(Stream<Event<E>> e) {
    if (e != null) {
      e.parallel().forEach(event -> {
        try {
          Future<R> result = onNext(event.value());
          CompletableFuture<R> future = futures.get(event.eventId());
          CompletableFuture.supplyAsync(() -> {
            try {
              future.complete(result.get());
            } catch (Exception e1) {
              onError(e1, event.value());
            }
            return null;
          });
        } catch (Throwable ex) {
          onError(ex, event.value());
        }
      });
    }
  }

  public abstract Future<R> onNext(E eventValue) throws Throwable;

  public abstract void onError(Throwable error, E eventValue);

  @Override
  public void finalise() {

  }

}
