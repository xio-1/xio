package org.xio.one.test;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.domain.flow.FutureItemFlowable;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.subscribers.FutureItemSubscriber;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;

import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.xio.one.reactive.flow.Flow.anItemFlow;

public class FunctionalStyleTest {

  Logger logger = Logger.getLogger(FunctionalStyleTest.class.getName());

  @BeforeClass
  public static void setup() {
    XIOService.start();
  }

  @AfterClass
  public static void tearDown() {
    XIOService.stop();
  }

  @Test
  public void toUpperCaseWithFunctionalStyle() throws Exception {
    ItemFlowable<String, String> toUPPERCASEFlow = anItemFlow("1");

    StringBuffer buff = new StringBuffer();

    Subscriber<String, String> upperCaseSubscriber =
        toUPPERCASEFlow
            .publishTo(ItemSubscriber.class)
            .doForEach(i -> buff.append(i.value().toUpperCase()).append(" "))
            .andOnEndReturn(() -> buff.toString().trim())
            .subscribe();

    toUPPERCASEFlow.putItem("value1", "value2", "value3", "value4", "value5");

    toUPPERCASEFlow.close(true);

    Assert.assertThat(upperCaseSubscriber.getFutureResult().get(),
        is("VALUE1 VALUE2 VALUE3 VALUE4 VALUE5"));
  }

  @Test
  public void toUpperCaseWithFunctionalWithPredicateExit() throws Exception {
    ItemFlowable<String, String> toUPPERCASEFlow = anItemFlow("2");

    StringBuffer buff = new StringBuffer();

    Subscriber<String, String> upperCaseSubscriber = toUPPERCASEFlow
        .publishTo(ItemSubscriber.class)
        .doForEach(i -> buff.append(i.value().toUpperCase()).append(" "))
        .whenPredicateExitAndReturn(p -> p.equals("value3"), () -> buff.toString().trim())
        .subscribe();

    toUPPERCASEFlow.putItem("value1", "value2", "value3", "value4", "value5");

    toUPPERCASEFlow.close(true);

    Assert.assertThat(upperCaseSubscriber.getFutureResult().get(), is("VALUE1 VALUE2"));
  }

  @Test
  public void toUpperCaseFutureResult() throws Exception {
    FutureItemFlowable<String, String> toUPPERCASEFlow = Flow.aFutureItemFlow();

    Subscriber<String, String> upperCaseSubscriber =
        toUPPERCASEFlow
            .publishTo(FutureItemSubscriber.class)
            .onStart(() -> logger.info("I am starting"))
            .returnForEach(i -> i.value().toUpperCase())
            .finallyOnEnd(() -> logger.info("I am done"))
            .subscribe();

    Assert
        .assertThat(toUPPERCASEFlow.submitItem("hello1").result(upperCaseSubscriber.getId()).get(),
            is("HELLO1"));
    Assert.assertThat(toUPPERCASEFlow.submitItem("hello2").results().get(0).get(), is("HELLO2"));
    Assert.assertThat(toUPPERCASEFlow.submitItem("hello3").results().get(0).get(), is("HELLO3"));

    toUPPERCASEFlow.close(true);

  }



}
