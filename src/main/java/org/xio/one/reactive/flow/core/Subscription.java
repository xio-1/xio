package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.core.domain.Item;
import org.xio.one.reactive.flow.core.domain.EmptyItem;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public final class Subscription<R, E> {

  private Item lastSeenItem = null;
  private Flow<E,R> itemStream;
  private Future subscription;
  private Subscriber<R, E> subscriber;

  public Subscription(Flow<E, R> itemStream, Subscriber<R, E> subscriber) {
    this.itemStream = itemStream;
    this.subscriber = subscriber;
  }

  public Flow getItemStream() {
    return itemStream;
  }

  public Future<R> subscribe() {
    subscriber.initialise();
    CompletableFuture<R> completableFuture = new CompletableFuture<>();
    this.subscription =
        itemStream
            .executorService()
            .submit(
                () -> {
                  while ((!itemStream.hasEnded() || !itemStream.contents().hasEnded())
                      && !subscriber.isDone()) {
                    processResults(subscriber);
                  }
                  processFinalResults(subscriber);
                  unsubscribe();
                  completableFuture.complete(subscriber.getNext());
                });
    return completableFuture;
  }

  private void processFinalResults(Subscriber<R, E> subscriber) {
    NavigableSet<Item<E>> streamContents = streamContents();
    while (streamContents.size() > 0) {
      subscriber.emit(streamContents.stream());
      streamContents = streamContents();
    }
  }

  private void processResults(Subscriber<R, E> subscriber) {
    NavigableSet<Item<E>> streamContents = streamContents();
    if (streamContents.size() > 0) subscriber.emit(streamContents.stream());
  }

  public void unsubscribe() {
    this.subscriber.stop();
  }

  protected NavigableSet<Item<E>> streamContents() {
    NavigableSet<Item<E>> streamContents =
        Collections.unmodifiableNavigableSet(itemStream.contents().allAfter(lastSeenItem));
    if (streamContents.size() > 0) lastSeenItem = streamContents.last();
    return streamContents;
  }

  public Subscriber<R, E> getSubscriber() {
    return subscriber;
  }

  public Item getLastSeenItem() {
    if (lastSeenItem!=null)
      return lastSeenItem;
    else
      return EmptyItem.EMPTY_ITEM;
  }
}
