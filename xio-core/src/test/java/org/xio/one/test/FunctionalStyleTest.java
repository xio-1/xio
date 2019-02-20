package org.xio.one.test;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.flow.domain.flow.ItemFlow;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;

import static org.hamcrest.CoreMatchers.is;
import static org.xio.one.reactive.flow.Flow.anItemFlow;

public class FunctionalStyleTest {

  @Test
  public void toUpperCaseWithFunctionalStyle() throws Exception {
    ItemFlow<String, String> toUPPERCASEFlow = anItemFlow();

    StringBuffer buff = new StringBuffer();

    Subscriber<String, String> upperCaseSubscriber =
        toUPPERCASEFlow.publish().doOnNext(i -> buff.append(i.value().toUpperCase()).append(" "))
            .onEndReturn(() -> buff.toString().trim()).subscribe();

    toUPPERCASEFlow.putItem("value1", "value2", "value3", "value4", "value5");

    toUPPERCASEFlow.close(true);

    Assert.assertThat(upperCaseSubscriber.getFutureResult().get(),
        is("VALUE1 VALUE2 VALUE3 VALUE4 VALUE5"));
  }

}
