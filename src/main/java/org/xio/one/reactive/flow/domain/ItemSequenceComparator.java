package org.xio.one.reactive.flow.domain;

import java.util.Comparator;

/**
 * Created by Admin on 09/09/2014.
 */
public class ItemSequenceComparator<T,R> implements Comparator<FlowItem<T,R>> {
  @Override
  public int compare(FlowItem o1, FlowItem o2) {
    if (o1 == o2)
      return 0;
    else if (o1.itemId() == o2.itemId())
      return 0;
    else if (o1.itemId() > o2.itemId())
      return 1;
    else if (o1.itemId() < o2.itemId())
      return -1;
    else
      return -1;
  }
}
