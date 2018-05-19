package org.xio.one.reactive.flow.domain;

public final class ItemComparator<T, R> extends FlowItem<T, R> {

  public ItemComparator(long itemId) {
    super(itemId);
  }

  @Override
  public boolean isAlive() {
    return false;
  }
}
