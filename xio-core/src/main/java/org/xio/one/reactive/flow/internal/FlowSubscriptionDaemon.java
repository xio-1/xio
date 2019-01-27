package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.EmptyItem;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.SubscriberInterface;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * SubscriptionService (manages subscribers processing)
 *
 * @Author Richard Durley
 * @OringinalWork XIO
 * @Copyright Richard Durley / XIO.ONE
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 */
public final class FlowSubscriptionDaemon<R, T> implements Runnable {

  Logger logger = Logger.getLogger(FlowSubscriptionDaemon.class.getCanonicalName());
  private Item lastSeenItem = null;
  private Flow<T, R> itemStream;
  private ArrayList<SubscriberInterface<R, T>> subscribers;
  private final Object locklist = new Object();

  public FlowSubscriptionDaemon(Flow<T, R> itemStream) {
    this.itemStream = itemStream;
    this.subscribers = new ArrayList<>();
  }

  public Future<R> subscribe(SubscriberInterface<R, T> subscriber) {
    subscriber.initialise();
    synchronized (locklist) {
      this.subscribers.add(subscriber);
    }
    return subscriber.getResult();
  }

  public void unsubscribe(SubscriberInterface<R, T> subscriber) {
      synchronized (locklist){
        subscriber.finalise();
        subscriber.stop();
        subscriber.setResult(subscriber.getNext());
        this.subscribers.remove(subscriber);
      }
  }

  public boolean hasActiveSubscribers() {
    return subscribers.stream().anyMatch(s->!s.isDone());
  }

  public void run() {
    while (!itemStream.hasEnded()) {
      List<Callable<Boolean>> callableList;
      synchronized (locklist) {
        callableList = subscribers.parallelStream().map(subscriber -> (Callable<Boolean>) () -> {
          if (!subscriber.isDone() && !itemStream.hasEnded()) {
            return processResults(subscriber);
          } else {
            processFinalResults(subscriber);
            subscriber.finalise();
            subscriber.stop();
            subscriber.setResult(subscriber.getNext());
            synchronized (locklist) {
              subscribers.remove(subscriber);
            }
            logger.log(Level.INFO,
                "Subscriber " + subscriber.getId() + " stopped for stream : " + itemStream.name());
            return true;
          }

        }).collect(Collectors.toList());
      }

      try {
        Optional<Boolean> hasExecuted =
            InternalExecutors.computeThreadPoolInstance().invokeAll(callableList).stream()
                .map(f -> {
                  try {
                    return f.get();
                  } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                  }
                  return Boolean.FALSE;
                }).filter(b -> b.equals(true)).findFirst();
        if (hasExecuted.isEmpty())
          LockSupport.parkNanos(10000);
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, "Subscriber loop was interrupted will try again");
      }
    }

  }

  private void processFinalResults(SubscriberInterface<R, T> subscriber) {
    NavigableSet<Item<T, R>> streamContents = streamContents(subscriber);
    while (streamContents.size() > 0) {
      subscriber.emit(streamContents);
      streamContents = streamContents(subscriber);
    }
  }

  private boolean processResults(SubscriberInterface<R, T> subscriber) {
    NavigableSet<Item<T, R>> streamContents = streamContents(subscriber);
    if (streamContents.size() > 0) {
      subscriber.emit(streamContents);
      return true;
    } else
      return false;
  }

  private NavigableSet<Item<T, R>> streamContents(SubscriberInterface<R, T> subscriber) {
    if (subscriber.delayMS() > 0)
      LockSupport.parkUntil(System.currentTimeMillis() + subscriber.delayMS());
    NavigableSet<Item<T, R>> streamContents = Collections
        .unmodifiableNavigableSet(this.itemStream.contents().allAfter(this.lastSeenItem));
    if (streamContents.size() > 0)
      lastSeenItem = streamContents.last();
    return streamContents;
  }

  public Optional<SubscriberInterface<R, T>> getSubscriber(String subscriberId) {
    return subscribers.stream().filter(s -> s.getId().equals(subscriberId)).findFirst();
  }

  public Item getLastSeenItem() {
    if (lastSeenItem != null)
      return lastSeenItem;
    else
      return EmptyItem.EMPTY_ITEM;
  }
}
