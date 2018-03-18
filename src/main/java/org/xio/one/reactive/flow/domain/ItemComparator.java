package org.xio.one.reactive.flow.domain;

public final class ItemComparator<T> extends Item<T> {

  public ItemComparator(long itemId) {
    super(itemId);
  }

  @Override
  public boolean isAlive() {
    return false;
  }
}
