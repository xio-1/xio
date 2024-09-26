package org.xio.one.reactive.flow.domain.item;

import java.util.Comparator;

/**
 * Created by Admin on 09/09/2014.
 */
public class ItemTimestampComparator implements Comparator<Item> {

  @Override
  public int compare(Item o1, Item o2) {
    if (o1 == o2) {
      return 0;
    } else if (o1.getItemTimestamp() > o2.getItemTimestamp()) {
      return 1;
    } else if (o1.getItemTimestamp() < o2.getItemTimestamp()) {
      return -1;
    } else if (o1.getItemTimestamp() == o2.getItemTimestamp() && o1.getItemId() == (o2.getItemId())) {
      return 0;
    } else if (o1.getItemId() > o2.getItemId()) {
      return 1;
    } else {
      return -1;
    }
  }
}
