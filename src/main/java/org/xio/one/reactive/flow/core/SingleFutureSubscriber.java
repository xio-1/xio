package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.domain.Item;

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

  public final Future<R> register(long itemId, CompletableFuture<R> completableFuture) {
    futures.put(itemId, completableFuture);
    return completableFuture;
  }

  @Override
  public final void process(Stream<Item<E>> e) {
    if (e != null) {
      e.parallel().forEach(item -> {
        try {
          Future<R> result = onNext(item.value());
          CompletableFuture<R> future = futures.get(item.itemId());
          CompletableFuture.supplyAsync(() -> {
            try {
              future.complete(result.get());
            } catch (Exception e1) {
              onError(e1, item.value());
            }
            return null;
          });
        } catch (Throwable ex) {
          onError(ex, item.value());
        }
      });
    }
  }

  public abstract Future<R> onNext(E itemValue) throws Throwable;

  public abstract void onError(Throwable error, E itemValue);

  @Override
  public void finalise() {

  }

}
