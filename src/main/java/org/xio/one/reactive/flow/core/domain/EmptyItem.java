package org.xio.one.reactive.flow.core.domain;

public final class EmptyItem extends Item {

  public static final Item EMPTY_ITEM = new EmptyItem();

  public EmptyItem() {
    super();
  }

  @Override
  public boolean isAlive() {
    return false;
  }

  @Override
  public String toString() {
    return "{\"domain\":\"EmptyItem\"}";
  }
}
