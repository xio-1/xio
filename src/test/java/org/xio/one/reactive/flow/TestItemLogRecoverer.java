package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.logging.ItemDeserializer;
import org.xio.one.reactive.flow.domain.item.logging.ItemLogFlowRecovery;

public class TestItemLogRecoverer implements ItemLogFlowRecovery<String, String> {

  @Override
  public void recoverAllItem(Flow<String, String> flow, ItemDeserializer<String> deserializer) {

  }
}
