package org.xio.one.reactive.flow.domain.item;

public final class EmptyItem extends Item {

  public static final Item EMPTY_ITEM = new EmptyItem();

  public EmptyItem() {
    super();
  }

  @Override
  public long itemId() {
    return 0;
  }

  @Override
  public boolean alive() {
    return false;
  }

  @Override
  public String toString() {
    return "{\"domain\":\"EmptyItem\"}";
  }
}
