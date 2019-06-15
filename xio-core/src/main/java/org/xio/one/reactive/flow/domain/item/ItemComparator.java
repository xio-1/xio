package org.xio.one.reactive.flow.domain.item;

public final class ItemComparator<T> extends Item<T> {

  public ItemComparator(long itemId) {
    super(itemId);
  }

  @Override
  public boolean alive() {
    return false;
  }
}
