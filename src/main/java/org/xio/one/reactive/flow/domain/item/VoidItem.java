package org.xio.one.reactive.flow.domain.item;

public final class VoidItem<T> extends Item<T> {

  public static final Item VOID_ITEM = new VoidItem();

  public VoidItem() {
    super();
  }

  @Override
  public long getItemId() {
    return 0;
  }

  @Override
  public boolean isAlive() {
    return false;
  }

  @Override
  public String toString() {
    return "{\"domain\":\"VoidItem\"}";
  }
}
