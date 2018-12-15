package org.xio.one.reactive.flow.domain;

public final class EmptyItem extends FlowItem {

  public static final FlowItem EMPTY_ITEM = new EmptyItem();

  public EmptyItem() {
    super();
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
