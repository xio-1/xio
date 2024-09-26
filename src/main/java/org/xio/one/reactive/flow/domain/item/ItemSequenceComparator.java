package org.xio.one.reactive.flow.domain.item;

import java.util.Comparator;

/**
 * Created by Admin on 09/09/2014.
 */
public class ItemSequenceComparator<T> implements Comparator<Item<T>> {

  @Override
  public int compare(Item o1, Item o2) {
    if (o1 == o2) {
      return 0;
    } else if (o1.getItemId() == o2.getItemId()) {
      return 0;
    } else if (o1.getItemId() > o2.getItemId()) {
      return 1;
    } else if (o1.getItemId() < o2.getItemId()) {
      return -1;
    } else {
      return -1;
    }
  }
}
