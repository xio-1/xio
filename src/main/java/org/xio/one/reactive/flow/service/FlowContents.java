package org.xio.one.reactive.flow.service;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.domain.ItemComparator;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;

import static org.xio.one.reactive.flow.domain.EmptyItem.EMPTY_ITEM;

/**
 * ItemStoreOperations @Author Xio @Copyright Xio
 */
public final class FlowContents<T, R> {

  public final NavigableSet<FlowItem<T, R>> EMPTY_ITEM_SET = new ConcurrentSkipListSet<>();
  private FlowService itemStore;
  private Flow itemStream;
  private NavigableSet<FlowItem<T, R>> itemStoreContents = null;

  public NavigableSet<FlowItem<T, R>> getItemStoreContents() {
    return itemStoreContents;
  }

  FlowContents(FlowService itemStore, Flow itemStream) {
    this.itemStore = itemStore;
    this.itemStream = itemStream;
    this.itemStoreContents = (NavigableSet<FlowItem<T, R>>) itemStore.itemRepositoryContents;
  }

  public FlowItem<T, R> item(long id) {
    return itemStoreContents.higher(new ItemComparator(id - 1));
  }

  public FlowItem item(Object index) {
    return (FlowItem) itemStore.getItemStoreIndexContents().get(index);
  }

  public FlowItem[] all() {
    List<FlowItem> items = new ArrayList<>(itemStoreContents);
    return items.toArray(new FlowItem[items.size()]);
  }

  public final NavigableSet<FlowItem<T, R>> allAfter(FlowItem lastItem) {
    try {
      NavigableSet<FlowItem<T, R>> querystorecontents =
          Collections.unmodifiableNavigableSet(this.itemStoreContents);
      if (querystorecontents.size() > 0)
        if (lastItem != null) {
          FlowItem newLastItem = querystorecontents.last();
          NavigableSet<FlowItem<T, R>> items = Collections.unmodifiableNavigableSet(
              querystorecontents.subSet(lastItem, false, newLastItem, true));
          if (items.size() > 0) {
            FlowItem newFirstItem = items.first();
            if (newFirstItem.itemId() == (lastItem.itemId() + 1)) {
              // if last domain is in correct sequence then
              if (newLastItem.itemId() == newFirstItem.itemId() + items.size() - 1)
                // if the size anItemFlow the domain to return is correct i.e. all in sequence
                if (items.size() == (newLastItem.itemId() + 1 - newFirstItem.itemId())) {
                  return items;
                }
              return extractItemsThatAreInSequence(lastItem, items, newFirstItem);
            } else
              LockSupport.parkNanos(100000);
          } else
            LockSupport.parkNanos(100000);
        } else
          return extractItemsThatAreInSequence(EMPTY_ITEM, querystorecontents,
              querystorecontents.first());
      else
        LockSupport.parkNanos(100000);
    } catch (NoSuchElementException | IllegalArgumentException e) {
    }
    return EMPTY_ITEM_SET;
  }

  public NavigableSet<FlowItem<T, R>> getAllAfterWithDelayMS(int delayMS, FlowItem lastseen) {
    long startTime = System.currentTimeMillis();
    NavigableSet<FlowItem<T, R>> toreturn = new ConcurrentSkipListSet<>();
    NavigableSet<FlowItem<T, R>> contents = this.allAfter(lastseen);
    toreturn.addAll(contents);
    if (contents.size() > 0) {
      lastseen = contents.last();
      while (System.currentTimeMillis() < startTime + delayMS) {
        contents = this.allAfter(lastseen);
        if (contents.size() > 0)
          toreturn.addAll(contents);
        else
          return EMPTY_ITEM_SET;
        LockSupport.parkNanos(100000);
      }
    } else {
      return EMPTY_ITEM_SET;
    }
    return toreturn;
  }

  private NavigableSet<FlowItem<T, R>> extractItemsThatAreInSequence(FlowItem lastItem,
      NavigableSet<FlowItem<T, R>> items, FlowItem newFirstItem) {
    FlowItem[] items1 = items.toArray(new FlowItem[items.size()]);
    int index = 0;
    FlowItem last = lastItem;
    FlowItem current = items1[0];
    while (current.itemId() == last.itemId() + 1 && index <= items1.length) {
      last = current;
      index++;
      if (index < items1.length)
        current = items1[index];
    }
    return items.subSet(newFirstItem, true, last, true);
  }

  public FlowItem<T, R> last() {
    NavigableSet<FlowItem<T, R>> querystorecontents =
        Collections.unmodifiableNavigableSet(this.itemStoreContents);
    if (querystorecontents.isEmpty())
      return EMPTY_ITEM;
    else {
      return querystorecontents.last();
    }
  }

  public boolean hasEnded() {
    return itemStore.hasEnded();
  }

  /*public Item getLastSeenItemByFieldnameValue(String fieldname, Object value) {
    Item toReturn = itemStore.itemStoreIndexContents.get(new ItemKey(fieldname, value));
    if (toReturn == null) {
      List<Item> domain =
          itemRepositoryContents.parallelStream().filter(u -> u.isAlive(itemTTLSeconds))
              .filter(i -> i.getFieldValue(fieldname).toString().equals(value))
              .collect(Collectors.toList());
      if (domain.size() > 0)
        return domain.get(domain.size() - 1);
    } else
      return toReturn;
    return EMPTY_ITEM;
  }*/

  public FlowItem first() {
    return this.itemStoreContents.first();
  }

  public Optional<FlowItem> nextFollowing(long itemid) {
    return Optional.ofNullable(this.itemStoreContents.higher(new ItemComparator(itemid)));
  }

  /*public Item getFirstBy(String fieldname, Object value) {
    List<Item> domain =
        itemRepositoryContents.parallelStream().filter(u -> u.isAlive(itemTTLSeconds))
            .filter(i -> i.getFieldValue(fieldname).equals(value)).collect(Collectors.toList());
    if (domain.size() > 0)
      return domain.get(0);
    else
      return EMPTY_ITEM;
  }*/

  /*public double getAverage(String fieldname) {
    try {
      return this.itemRepositoryContents.parallelStream()
          .filter(u -> u.isAlive(itemTTLSeconds) && u.hasFieldValue(fieldname))
          .mapToDouble(e -> (double) e.getFieldValue(fieldname)).average().getAsDouble();
    } catch (NoSuchElementException e) {
      return 0.0d;
    }
  }*/

  /*public Item getMax(String fieldname) {
    try {

      Item domain;
      domain = itemRepositoryContents.parallelStream()
          .filter(u -> u.isAlive(itemTTLSeconds) && u.hasFieldValue(fieldname))
          .max(Comparator.comparing(i -> (double) i.getFieldValue(fieldname))).get();
      return domain;
    } catch (NoSuchElementException e) {
      return EMPTY_ITEM;
    }
  }

  public Item getMin(String fieldname) {
    try {
      Item domain;
      domain = itemRepositoryContents.parallelStream()
          .filter(u -> u.isAlive(itemTTLSeconds) && u.hasFieldValue(fieldname))
          .min(Comparator.comparing(i -> (double) i.getFieldValue(fieldname))).get();
      return domain;
    } catch (NoSuchElementException e) {
      return EMPTY_ITEM;
    }
  }

  public double getSum(String fieldname) {
    try {
      return this.itemRepositoryContents.parallelStream()
          .filter(u -> u.isAlive(itemTTLSeconds) && u.hasFieldValue(fieldname))
          .mapToDouble(e -> (double) e.getFieldValue(fieldname)).sum();
    } catch (NoSuchElementException e) {
      return 0.0d;
    }
  }

  public Set<Map.Entry<Object, Optional<Item>>> getLastSeenItemGroupedByFieldnameValue(
      String fieldname) {
    return itemRepositoryContents.parallelStream()
        .filter(u -> u.isAlive(itemTTLSeconds) && u.hasFieldValue(fieldname)).collect(
            Collectors.groupingBy(foo -> foo.getFieldValue(fieldname),
                Collectors.maxBy(new ItemTimestampComparator()))).entrySet();

  }

    public Item[] getAllBy(String fieldName, Object value) {
    List<Item> domain = itemRepositoryContents.parallelStream().filter(
        u -> u.isAlive(itemTTLSeconds) && u.getFieldValue(fieldName).toString().equals(value))
        .collect(Collectors.toList());
    return domain.toArray(new Item[domain.size()]);
  }

  */

  public NavigableSet<FlowItem<T, R>> getTimeWindowSet(long from, long to) throws FlowException {
    // Get first and last domain in the window
    // otherwise return empty set as nothing found in the window
    return EMPTY_ITEM_SET;
  }
}
