package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.ItemSequenceComparator;
import org.xio.one.reactive.flow.subscribers.internal.SubscriptionService;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.util.Collection;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FlowDaemonService (Flow input and housekeeping)
 *
 * @Author Richard Durley
 * @OringinalWork XIO
 * @Copyright Richard Durley / XIO.ONE
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 */
public final class FlowService<T, R> {

  protected volatile ConcurrentSkipListSet<Item<T, R>> itemRepositoryContents;
  protected volatile ConcurrentHashMap<Object, Item<T, R>> itemStoreIndexContents;
  Logger logger = Logger.getLogger(FlowService.class.getCanonicalName());
  private Flow itemStream = null;
  private boolean isEnd = false;
  private FlowContents itemStoreOperations = null;
  private String itemStoreIndexFieldName;
  private static Future flowInputDaemon = null;


  /**
   * New Item BaseWorker Execution using the sequence comparator to order the results
   * by item sequence number.
   * Items will be retained until consumed to by all subscribers and whilst they are alive
   * i.e. before they expire their/stream TTL
   */
  public FlowService(Flow<T, R> itemStream) {
    this.itemStream = itemStream;
    itemRepositoryContents = new ConcurrentSkipListSet<>(new ItemSequenceComparator<>());
    itemStoreOperations = new FlowContents<T, R>(this, itemStream);
    itemStoreIndexContents = new ConcurrentHashMap<>();
    if (itemStream.indexFieldName() != null) {
      itemStoreIndexFieldName = itemStream.indexFieldName();
    }
    startFlowInputDaemon();
    InternalExecutors.itemLoopThreadPoolInstance().submit(new ExpiredItemsCollector());
  }

  private static synchronized void startFlowInputDaemon() {
    if (flowInputDaemon==null || flowInputDaemon.isDone() || flowInputDaemon.isCancelled())
      flowInputDaemon  = InternalExecutors.itemLoopThreadPoolInstance().submit(new FlowInputDaemon());
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

  public boolean hasEnded() {
    return this.isEnd;
  }

  public Collection<Item<T, R>> getItemRepositoryContents() {
    return itemRepositoryContents;
  }

  public ConcurrentHashMap<Object, Item<T, R>> getItemStoreIndexContents() {
    return itemStoreIndexContents;
  }

  public long getMinimumLastSeenProcessed(Flow itemStream) {
    Map<String, SubscriptionService> subscriptionMap = itemStream.getSubscriberSubscriptions();
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
   * Gets all the input from the Xio.contents.domain itemStream and persists it to the contents store
   */
  /*private class FlowInput implements Runnable {

    FlowService daemon;

    public FlowInput(FlowService daemon) {
      this.daemon = daemon;
    }

    @Override
    public void run() {
      logger.log(Level.INFO, "Flow input thread started for stream : " + itemStream.name());
      try {
        while (!itemStream.hasEnded()) {
          itemStream.acceptAll();
        }
        //itemRepositoryContents.last().itemId() > getMinimumLastSeenProcessed(itemStream)
        while (!itemStream.isEmpty())
          LockSupport.parkNanos(100000);
        daemon.isEnd = true;
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Housekeeping thread ended unexpectedly", e);
      }
      logger.log(Level.INFO, "Flow input stopped for stream : " + itemStream.name());
    }
  }*/


  /**
   * Removes seen dead domain from the contents store
   */
  private class ExpiredItemsCollector implements Runnable {

    @Override
    public void run() {
      try {
        logger.log(Level.INFO,
            "House keeping thread started for stream : " + itemStream.name() + " every :" + (
                (itemStream.ttl() * 1000) / 2) + " ms");
        while (!itemStream.hasEnded()) {
          Thread.sleep(itemStream.ttl());
          if (!itemRepositoryContents.isEmpty()) {
            //long lastSeenItemId = getMinimumLastSeenProcessed(itemStream);
            if (itemRepositoryContents.removeIf(item -> !item.alive()))
              logger.log(Level.INFO, "House kept stream : " + itemStream.name());
          }
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Housekeeping thread ended unexpectedly", e);
      }
      logger.log(Level.INFO, "Housekeeping stopped for stream : " + itemStream.name());
    }

  }
}
