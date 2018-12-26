package org.xio.one.reactive.flow.service;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.ItemSequenceComparator;
import org.xio.one.reactive.flow.subscriber.internal.Subscription;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;

/**
 * The Xio.contents.domain itemQueryStore where the putAll domain are persisted in memory
 */
public final class FlowDaemonService<T, R> {

  protected volatile ConcurrentSkipListSet<Item<T, R>> itemRepositoryContents;
  protected volatile ConcurrentHashMap<Object, Item<T, R>> itemStoreIndexContents;
  private Flow itemStream = null;
  private boolean isEnd = false;
  private FlowContents itemStoreOperations = null;
  private String itemStoreIndexFieldName;

  /**
   * New Item BaseWorker Execution using the sequence comparator to order the results
   * by item sequence number.
   * Items will be retained until consumed to by all subscriber and whilst they are alive
   * i.e. before they expire their/stream TTL
   */
  public FlowDaemonService(Flow<T, R> itemStream) {
    this.itemStream = itemStream;
    itemRepositoryContents = new ConcurrentSkipListSet<>(new ItemSequenceComparator<>());
    itemStoreOperations = new FlowContents<T, R>(this, itemStream);
    itemStoreIndexContents = new ConcurrentHashMap<>();
    if (itemStream.indexFieldName() != null) {
      itemStoreIndexFieldName = itemStream.indexFieldName();
    }
    InternalExecutors.itemLoopThreadPoolInstance().submit(new ExpiredItemsCollector());
    InternalExecutors.itemLoopThreadPoolInstance().submit(new FlowInput(this));
  }

  public void setItemStoreIndexFieldName(String itemStoreIndexFieldName) {
    this.itemStoreIndexFieldName = itemStoreIndexFieldName;
  }

  /**
   * Get the contents stores contents operations
   *
   * @return
   */
  public FlowContents<T, R> query() {
    return itemStoreOperations;
  }

  /**
   * Do work
   *
   * @param items
   */
  private Item work(Item[] items) {
    Arrays.stream(items).forEach(item -> {
      itemRepositoryContents.add(item);
      if (getItemStoreIndexFieldName() != null)
        itemStoreIndexContents.put(item.getFieldValue(getItemStoreIndexFieldName()), item);
    });
    if (items.length > 0)
      return items[items.length - 1];
    else
      return null;
  }

  public boolean hasEnded() {
    return this.isEnd;
  }

  public Collection<Item<T, R>> getItemRepositoryContents() {
    return itemRepositoryContents;
  }

  public String getItemStoreIndexFieldName() {
    return itemStoreIndexFieldName;
  }

  public ConcurrentHashMap<Object, Item<T, R>> getItemStoreIndexContents() {
    return itemStoreIndexContents;
  }

  /**
   * Gets all the input from the Xio.contents.domain itemStream and persists it to the contents store
   */
  private class FlowInput implements Runnable {

    FlowDaemonService daemon;

    public FlowInput(FlowDaemonService daemon) {
      this.daemon = daemon;
    }

    @Override
    public void run() {
      try {
        while (!itemStream.hasEnded()) {
          itemStream.acceptAll();
        }

        while ((!itemStream.isEmpty()
            && itemRepositoryContents.last().itemId() > getMinimumLastSeenProcessed(itemStream))
            || itemRepositoryContents.size() != itemStream.size())
          LockSupport.parkNanos(100000);
        daemon.isEnd = true;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  private long getMinimumLastSeenProcessed(Flow itemStream) {
    Map<String, Subscription> subscriptionMap = itemStream.getSubscriberSubscriptions();
    if (subscriptionMap.size() > 0) {
      OptionalLong lastSeenItemId = subscriptionMap.entrySet().stream()
          .mapToLong(e -> e.getValue().getLastSeenItem().itemId()).min();
      if (lastSeenItemId.isPresent())
        return lastSeenItemId.getAsLong();
      else
        return Long.MAX_VALUE;
    } else
      return Long.MAX_VALUE;
  }

  /**
   * Removes seen dead domain from the contents store
   */
  private class ExpiredItemsCollector implements Runnable {

    @Override
    public void run() {
      try {
        while (!itemStream.hasEnded()) {
          Thread.currentThread().sleep(1000);
          if (!itemRepositoryContents.isEmpty()) {
            long lastSeenItemId = getMinimumLastSeenProcessed(itemStream);
            itemRepositoryContents.removeIf(item -> !item.alive(lastSeenItemId));
          }
        }
      } catch (Exception e) {
        e.printStackTrace();

      }
    }

  }
}