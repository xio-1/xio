package org.xio.one.reactive.flow;

import static org.xio.one.reactive.flow.Flow.anItemFlow;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.item.logging.AsyncCallbackItemLoggerService;
import org.xio.one.reactive.flow.domain.item.logging.ItemDeserializer;
import org.xio.one.reactive.flow.domain.item.logging.ItemLogRecoverer;

public class TestItemLogRecovery implements ItemLogRecoverer<String, String> {

  public TestItemLogRecovery(String filename) {


  }


  @Override
  public void recoverAllItems(Flow<String, String> flow, ItemDeserializer<String> deserializer) {
    ItemFlowable<String, Long> stream = anItemFlow("ping_stream", new AsyncCallbackItemLoggerService<>("recovery-test.dat", new ObjectToByteArrayJSONSerializer<>(),1024*1024*2,"\n".getBytes()));
    stream.putItem("1","2","3","4");
    stream.takeRecoverySnapshot(true);
    stream.close(true);

  }
}
