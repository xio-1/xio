package org.xio.one.reactive.flow.domain.item;

public final class VoidItem extends Item {

  public static final Item VOID_ITEM = new VoidItem();

  public VoidItem() {
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
    return "{\"domain\":\"VoidItem\"}";
  }
}
