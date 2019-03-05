package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.domain.FlowException;
import org.xio.one.reactive.flow.domain.item.EmptyItem;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.ItemComparator;
import org.xio.one.reactive.flow.domain.item.ItemSequenceComparator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.LockSupport;

import static org.xio.one.reactive.flow.domain.item.EmptyItem.EMPTY_ITEM;

/**
 * FlowContents (Query Store)
 *
 * @Author Richard Durley
 * @OringinalWork XIO
 * @Copyright Richard Durley / XIO.ONE
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 */
public final class FlowContents<T, R> {


  public final NavigableSet<Item<T, R>> EMPTY_ITEM_SET = new ConcurrentSkipListSet<>();
  //protected volatile ConcurrentSkipListSet<Item<T, R>> itemRepositoryContents;
  protected volatile ConcurrentHashMap<Object, Item<T, R>> itemStoreIndexContents;
  protected volatile NavigableSet<Item<T, R>> itemStoreContents = null;
  private String itemStoreIndexFieldName;
  private Flow itemFlow;

  public FlowContents(Flow itemStream) {
    this.itemFlow = itemStream;
    itemStoreContents = new ConcurrentSkipListSet<>(new ItemSequenceComparator<>());
    itemStoreIndexContents = new ConcurrentHashMap<>();
    if (itemFlow.indexFieldName() != null) {
      itemStoreIndexFieldName = itemFlow.indexFieldName();
    }
  }

  public NavigableSet<Item<T, R>> getItemStoreContents() {
    return this.itemStoreContents;
  }

  public Item<T, R> item(long id) {
    return itemStoreContents.higher(new ItemComparator(id - 1));
  }

  public Item item(Object index) {
    return itemStoreIndexContents.get(index);
  }

  public Item[] all() {
    List<Item> items = new ArrayList<>(itemStoreContents);
    return items.toArray(new Item[items.size()]);
  }

  public Object[] allValues() {
    return itemStoreContents.stream().filter(Item::alive).map(Item::value).toArray();
  }

  public NavigableSet<Item<T, R>> allBefore(Item lastItem) {
    return this.itemStoreContents.subSet(this.itemStoreContents.first(), true, lastItem, false);
  }


  public final NavigableSet<Item<T, R>> allAfter(Item lastItem) {
    try {
      NavigableSet<Item<T, R>> querystorecontents =
          Collections.unmodifiableNavigableSet(this.itemStoreContents);
      if (!querystorecontents.isEmpty())
        if (!EmptyItem.EMPTY_ITEM.equals(lastItem) && lastItem != null) {
          Item newLastItem = querystorecontents.last();
          NavigableSet<Item<T, R>> items = Collections.unmodifiableNavigableSet(
              querystorecontents.subSet(lastItem, false, newLastItem, true));
          if (!items.isEmpty()) {
            Item newFirstItem = items.first();
            if (newFirstItem.itemId() == (lastItem.itemId() + 1)) {
              // if last domain is in correct sequence then
              final int size = items.size();
              if (newLastItem.itemId() == newFirstItem.itemId() + size - 1)
                // if the size anItemFlow the domain to return is correct i.e. all in sequence
                if (size == (newLastItem.itemId() + 1 - newFirstItem.itemId())) {
                  return items;
                }
              return extractItemsThatAreInSequence(lastItem, items, newFirstItem);
            } else
              LockSupport.parkNanos(100000);
          } else
            LockSupport.parkNanos(100000);
        } else
          return extractItemsThatAreInSequence(querystorecontents.last(), querystorecontents,
              querystorecontents.first());
      else
        LockSupport.parkNanos(100000);
    } catch (NoSuchElementException | IllegalArgumentException e) {
    }
    return EMPTY_ITEM_SET;
  }

  public NavigableSet<Item<T, R>> getAllAfterWithDelayMS(int delayMS, Item lastseen) {
    long startTime = System.currentTimeMillis();
    NavigableSet<Item<T, R>> toreturn = new ConcurrentSkipListSet<>();
    NavigableSet<Item<T, R>> contents = this.allAfter(lastseen);
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

  private NavigableSet<Item<T, R>> extractItemsThatAreInSequence(Item lastItem,
      NavigableSet<Item<T, R>> items, Item newFirstItem) {
    Item[] items1 = items.toArray(new Item[items.size()]);
    int index = 0;
    Item last = lastItem;
    Item current = items1[0];
    while (current.itemId() == last.itemId() + 1 && index <= items1.length) {
      last = current;
      index++;
      if (index < items1.length)
        current = items1[index];
    }
    return items.subSet(newFirstItem, true, last, true);
  }

  public Item<T, R> last() {
    NavigableSet<Item<T, R>> querystorecontents =
        Collections.unmodifiableNavigableSet(this.itemStoreContents);
    if (querystorecontents.isEmpty())
      return EMPTY_ITEM;
    else {
      Optional<Item<T, R>> last =
          querystorecontents.descendingSet().stream().filter(Item::alive).findFirst();
      return last.orElse(EMPTY_ITEM);
    }
  }

  /*public Item getLastSeenItemByFieldnameValue(String fieldname, Object value) {
    Item toReturn = daemon.itemStoreIndexContents.get(new ItemKey(fieldname, value));
    if (toReturn == null) {
      List<Item> domain =
          itemRepositoryContents.parallelStream().filter(u -> u.alive(itemTTLSeconds))
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
        itemRepositoryContents.parallelStream().filter(u -> u.alive(itemTTLSeconds))
            .filter(i -> i.getFieldValue(fieldname).equals(value)).collect(Collectors.toList());
    if (domain.size() > 0)
      return domain.get(0);
    else
      return EMPTY_ITEM;
  }*/

  /*public double getAverage(String fieldname) {
    try {
      return this.itemRepositoryContents.parallelStream()
          .filter(u -> u.alive(itemTTLSeconds) && u.hasFieldValue(fieldname))
          .mapToDouble(e -> (double) e.getFieldValue(fieldname)).average().getAsDouble();
    } catch (NoSuchElementException e) {
      return 0.0d;
    }
  }*/

  /*public Item getMax(String fieldname) {
    try {

      Item domain;
      domain = itemRepositoryContents.parallelStream()
          .filter(u -> u.alive(itemTTLSeconds) && u.hasFieldValue(fieldname))
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
          .filter(u -> u.alive(itemTTLSeconds) && u.hasFieldValue(fieldname))
          .min(Comparator.comparing(i -> (double) i.getFieldValue(fieldname))).get();
      return domain;
    } catch (NoSuchElementException e) {
      return EMPTY_ITEM;
    }
  }

  public double getSum(String fieldname) {
    try {
      return this.itemRepositoryContents.parallelStream()
          .filter(u -> u.alive(itemTTLSeconds) && u.hasFieldValue(fieldname))
          .mapToDouble(e -> (double) e.getFieldValue(fieldname)).sum();
    } catch (NoSuchElementException e) {
      return 0.0d;
    }
  }

  public Set<Map.Entry<Object, Optional<Item>>> getLastSeenItemGroupedByFieldnameValue(
      String fieldname) {
    return itemRepositoryContents.parallelStream()
        .filter(u -> u.alive(itemTTLSeconds) && u.hasFieldValue(fieldname)).collect(
            Collectors.groupingBy(foo -> foo.getFieldValue(fieldname),
                Collectors.maxBy(new ItemTimestampComparator()))).entrySet();

  }

    public Item[] getAllBy(String fieldName, Object value) {
    List<Item> domain = itemRepositoryContents.parallelStream().filter(
        u -> u.alive(itemTTLSeconds) && u.getFieldValue(fieldName).toString().equals(value))
        .collect(Collectors.toList());
    return domain.toArray(new Item[domain.size()]);
  }

  */

  public NavigableSet<Item<T, R>> getTimeWindowSet(long from, long to) throws FlowException {
    // Get first and last domain in the window
    // otherwise return empty set as nothing found in the window
    return EMPTY_ITEM_SET;
  }
}
