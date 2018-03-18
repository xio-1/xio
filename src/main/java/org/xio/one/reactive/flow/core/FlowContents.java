package org.xio.one.reactive.flow.core;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.Item;
import org.xio.one.reactive.flow.domain.ItemComparator;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;

import static org.xio.one.reactive.flow.domain.EmptyItem.EMPTY_ITEM;

/** ItemStoreOperations @Author Xio @Copyright Xio */
public final class FlowContents<T> {

  public final NavigableSet<Item<T>> EMPTY_ITEM_SET = new ConcurrentSkipListSet<>();
  private FlowContentsControl itemStore;
  private Flow itemStream;
  private NavigableSet<Item<T>> itemStoreContents = null;

  FlowContents(FlowContentsControl itemStore, Flow itemStream) {
    this.itemStore = itemStore;
    this.itemStream = itemStream;
    this.itemStoreContents = (NavigableSet<Item<T>>) itemStore.itemRepositoryContents;
  }

  public Item<T> item(long id) {
    return itemStoreContents.higher(new ItemComparator(id - 1));
  }

  public Item item(Object index) {
    return (Item) itemStore.getItemStoreIndexContents().get(index);
  }

  public long count() {
    return itemStoreContents.size();
  }

  public Item[] all() {
    List<Item> items = new ArrayList<>(itemStoreContents);
    return items.toArray(new Item[items.size()]);
  }

  protected final NavigableSet<Item<T>> allAfter(Item lastItem) {
    try {
      NavigableSet<Item<T>> querystorecontents =
          Collections.unmodifiableNavigableSet(this.itemStoreContents);
      if (lastItem != null) {
        Item newLastItem = querystorecontents.last();
        NavigableSet<Item<T>> items =
            Collections.unmodifiableNavigableSet(
                querystorecontents.subSet(lastItem, false, newLastItem, true));
        if (items.size() > 0) {
          Item newFirstItem = items.first();
          if (newFirstItem.itemId() == (lastItem.itemId() + 1)) {
            // if last domain is in correct sequence then
            if (newLastItem.itemId() == newFirstItem.itemId() + items.size() - 1)
              // if the size flow the domain to return is correct i.e. all in sequence
              if (items.size() == (newLastItem.itemId() + 1 - newFirstItem.itemId())) {
                return items;
              }
            return extractItemsThatAreInSequence(lastItem, items, newFirstItem);
          } else LockSupport.parkNanos(100000);
        } else LockSupport.parkNanos(100000);
      } else
        return extractItemsThatAreInSequence(
            EMPTY_ITEM, querystorecontents, querystorecontents.first());
    } catch (NoSuchElementException | IllegalArgumentException e) {
    }
    return EMPTY_ITEM_SET;
  }

  private NavigableSet<Item<T>> extractItemsThatAreInSequence(
      Item lastItem, NavigableSet<Item<T>> items, Item newFirstItem) {
    Item[] items1 = items.toArray(new Item[items.size()]);
    int index = 0;
    Item last = lastItem;
    Item current = items1[0];
    while (current.itemId() == last.itemId() + 1 && index <= items1.length) {
      last = current;
      index++;
      if (index < items1.length) current = items1[index];
    }
    return items.subSet(newFirstItem, true, last, true);
  }

  public Item<T> last() {
    NavigableSet<Item<T>> querystorecontents =
        Collections.unmodifiableNavigableSet(this.itemStoreContents);
    if (querystorecontents.isEmpty()) return EMPTY_ITEM;
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

  public Item first() {
    return this.itemStoreContents.first();
  }

  public Optional<Item> nextFollowing(long itemid) {
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

  public NavigableSet<Item<T>> getTimeWindowSet(long from, long to)
      throws FlowException {
    // Get first and last domain in the window
    // otherwise return empty set as nothing found in the window
    return EMPTY_ITEM_SET;
  }
}
