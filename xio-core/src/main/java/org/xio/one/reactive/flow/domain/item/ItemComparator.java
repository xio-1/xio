package org.xio.one.reactive.flow.domain.item;

public final class ItemComparator<T, R> extends Item<T, R> {

  public ItemComparator(long itemId) {
    super(itemId);
  }

  @Override
  public boolean alive() {
    return false;
  }
}
