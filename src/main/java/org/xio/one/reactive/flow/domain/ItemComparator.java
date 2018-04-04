package org.xio.one.reactive.flow.domain;

public final class ItemComparator<T> extends FlowItem<T> {

  public ItemComparator(long itemId) {
    super(itemId);
  }

  @Override
  public boolean isAlive() {
    return false;
  }
}
