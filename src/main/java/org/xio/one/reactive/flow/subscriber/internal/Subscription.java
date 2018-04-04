package org.xio.one.reactive.flow.subscriber.internal;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.domain.EmptyItem;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public final class Subscription<R, E> {

  private FlowItem lastSeenItem = null;
  private Flow<E,R> itemStream;
  private Future subscription;
  private SubscriberOperations<R, E> subscriber;

  public Subscription(Flow<E, R> itemStream, SubscriberOperations<R, E> subscriber) {
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

  private void processFinalResults(SubscriberOperations<R, E> subscriber) {
    NavigableSet<FlowItem<E>> streamContents = streamContents();
    while (streamContents.size() > 0) {
      subscriber.emit(streamContents);
      streamContents = streamContents();
    }
  }

  private void processResults(SubscriberOperations<R, E> subscriber) {
    NavigableSet<FlowItem<E>> streamContents = streamContents();
    if (streamContents.size() > 0) subscriber.emit(streamContents);
  }

  private void unsubscribe() {
    this.subscriber.stop();
  }

  private NavigableSet<FlowItem<E>> streamContents() {
    NavigableSet<FlowItem<E>> streamContents =
        Collections.unmodifiableNavigableSet(itemStream.contents().allAfter(lastSeenItem));
    if (streamContents.size() > 0) lastSeenItem = streamContents.last();
    return streamContents;
  }

  public SubscriberOperations<R, E> getSubscriber() {
    return subscriber;
  }

  public FlowItem getLastSeenItem() {
    if (lastSeenItem!=null)
      return lastSeenItem;
    else
      return EmptyItem.EMPTY_ITEM;
  }
}