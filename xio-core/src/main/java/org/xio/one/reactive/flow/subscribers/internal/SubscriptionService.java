package org.xio.one.reactive.flow.subscribers.internal;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.EmptyItem;
import org.xio.one.reactive.flow.domain.item.Item;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SubscriptionService (manages subscribers processing)
 *
 * @Author Richard Durley
 * @OringinalWork XIO.ONE
 * @Copyright Richard Durley
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 *
 */
public final class SubscriptionService<R, T> {

  Logger logger = Logger.getLogger(SubscriptionService.class.getCanonicalName());
  private Item lastSeenItem = null;
  private Flow<T, R> itemStream;
  private SubscriberInterface<R, T> subscriber;

  public SubscriptionService(Flow<T, R> itemStream, SubscriberInterface<R, T> subscriber) {
    this.itemStream = itemStream;
    this.subscriber = subscriber;
  }

  public Future<R> subscribe() {
    subscriber.initialise();
    CompletableFuture<R> completableFuture = new CompletableFuture<>();
    itemStream.executorService().submit(() -> {
      logger.log(Level.INFO,
          "Subscriber " + subscriber.getId() + " started for stream : " + itemStream.name());
      while (!itemStream.hasEnded() && !subscriber.isDone()) {
        processResults(subscriber);
      }
      processFinalResults(subscriber);
      unsubscribe();
      logger.log(Level.INFO,
          "Subscriber " + subscriber.getId() + " stopped for stream : " + itemStream.name());
      completableFuture.complete(subscriber.getNext());
    });
    return completableFuture;
  }

  private void processFinalResults(SubscriberInterface<R, T> subscriber) {
    NavigableSet<Item<T, R>> streamContents = streamContents();
    while (streamContents.size() > 0) {
      subscriber.emit(streamContents);
      streamContents = streamContents();
    }
  }

  private void processResults(SubscriberInterface<R, T> subscriber) {
    NavigableSet<Item<T, R>> streamContents = streamContents();
    if (streamContents.size() > 0)
      subscriber.emit(streamContents);
  }

  private void unsubscribe() {
    if (!this.subscriber.isDone())
      this.subscriber.stop();
  }

  private NavigableSet<Item<T, R>> streamContents() {
    if (this.subscriber.delayMS() > 0)
      LockSupport.parkUntil(System.currentTimeMillis() + this.subscriber.delayMS());
    NavigableSet<Item<T, R>> streamContents = Collections
        .unmodifiableNavigableSet(this.itemStream.contents().allAfter(this.lastSeenItem));
    if (streamContents.size() > 0)
      lastSeenItem = streamContents.last();
    return streamContents;
  }

  public SubscriberInterface<R, T> getSubscriber() {
    return subscriber;
  }

  public Item getLastSeenItem() {
    if (lastSeenItem != null)
      return lastSeenItem;
    else
      return EmptyItem.EMPTY_ITEM;
  }
}