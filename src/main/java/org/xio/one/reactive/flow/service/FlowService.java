package org.xio.one.reactive.flow.service;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.subscribers.Subscription;
import org.xio.one.reactive.flow.util.InternalExecutors;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.domain.ItemSequenceComparator;

import java.util.Arrays;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;

/**
 * The Xio.contents.domain itemQueryStore where the putAll domain are persisted in memory
 */
public final class FlowService<T> {

  protected volatile ConcurrentSkipListSet<FlowItem<T>> itemRepositoryContents;
  protected volatile ConcurrentHashMap<Object, FlowItem<T>> itemStoreIndexContents;
  private Flow itemStream = null;
  private boolean isEnd = false;
  private FlowContents itemStoreOperations = null;
  private String itemStoreIndexFieldName;

  /**
   * New Item BaseWorker Execution using the sequence comparator to order the results
   * by item sequence number.
   * Items will be retained until consumed to by all subscribers and whilst they are alive
   * i.e. before they expire their/stream TTL
   */
  public FlowService(Flow itemStream) {
    this.itemStream = itemStream;
    itemRepositoryContents = new ConcurrentSkipListSet<>(new ItemSequenceComparator<>());
    itemStoreOperations = new FlowContents<FlowItem<T>>(this, itemStream);
    itemStoreIndexContents = new ConcurrentHashMap<>();
    if (itemStream.indexFieldName() != null) {
      itemStoreIndexFieldName = itemStream.indexFieldName();
    }
    InternalExecutors.itemLoopThreadPoolInstance().submit(new ExpiredItemsCollector());
    InternalExecutors.itemLoopThreadPoolInstance().submit(new WorkerInput(this));
  }

  public void setItemStoreIndexFieldName(String itemStoreIndexFieldName) {
    this.itemStoreIndexFieldName = itemStoreIndexFieldName;
  }

  /**
   * Get the contents stores contents operations
   *
   * @return
   */
  public FlowContents<FlowItem<T>> query() {
    return itemStoreOperations;
  }

  /**
   * Do work
   *
   * @param items
   */
  private FlowItem work(FlowItem[] items) {
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


  public String getItemStoreIndexFieldName() {
    return itemStoreIndexFieldName;
  }

  public ConcurrentHashMap<Object, FlowItem<T>> getItemStoreIndexContents() {
    return itemStoreIndexContents;
  }

  /**
   * Gets all the input from the Xio.contents.domain itemStream and persists it to the contents store
   */
  private class WorkerInput implements Runnable {

    FlowService itemStore;

    public WorkerInput(FlowService itemStore) {
      this.itemStore = itemStore;
    }

    @Override
    public void run() {
      try {
        FlowItem last = null;
        boolean hasRunatLeastOnce = false;
        while (!itemStream.hasEnded() || !hasRunatLeastOnce) {
          FlowItem next_last = this.itemStore.work(itemStream.takeAll());
          if (next_last != null)
            last = next_last;
          hasRunatLeastOnce = true;
        }
        FlowItem next_last = this.itemStore.work(itemStream.takeAll());
        if (next_last != null)
          last = next_last;
        if (last != null)
          while (!last.equals(this.itemStore.itemStoreOperations.last()) || (last.itemId()
              > getMinimumLastSeenProcessed(itemStream)))
            LockSupport.parkNanos(100000);
        itemStore.isEnd = true;
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
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
            itemRepositoryContents.removeIf(item -> !item.isAlive(lastSeenItemId));
          }
        }
      } catch (Exception e) {

      }
    }

  }
}
