package org.xio.one.reactive.flow.core.domain;

import java.util.Comparator;

/**
 * Created by Admin on 09/09/2014.
 */
public class ItemTimestampComparator implements Comparator<Item> {
  @Override
  public int compare(Item o1, Item o2) {
    if (o1 == o2)
      return 0;
    else if (o1.itemTimestamp() > o2.itemTimestamp())
      return 1;
    else if (o1.itemTimestamp() < o2.itemTimestamp())
      return -1;
    else if (o1.itemTimestamp() == o2.itemTimestamp() && o1.itemId() == (o2
        .itemId()))
      return 0;
    else if (o1.itemId() > o2.itemId())
      return 1;
    else
      return -1;
  }
}
