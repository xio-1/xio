package org.xio.one.test;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.FutureItemFlowable;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.flow.Promise;
import org.xio.one.reactive.flow.subscribers.FutureItemSubscriber;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;

import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.xio.one.reactive.flow.Flow.anItemFlow;

public class FunctionalStyleTest {

  @Test
  public void toUpperCaseWithFunctionalStyle() throws Exception {
    ItemFlowable<String, String> toUPPERCASEFlow = anItemFlow();

    StringBuffer buff = new StringBuffer();

    Subscriber<String, String> upperCaseSubscriber = toUPPERCASEFlow.publishTo(ItemSubscriber.class)
        .forEach(i -> buff.append(i.value().toUpperCase()).append(" "))
        .onEndReturn(() -> buff.toString().trim()).subscribe();

    toUPPERCASEFlow.putItem("value1", "value2", "value3", "value4", "value5");

    toUPPERCASEFlow.close(true);

    Assert.assertThat(upperCaseSubscriber.getFutureResult().get(),
        is("VALUE1 VALUE2 VALUE3 VALUE4 VALUE5"));
  }

  @Test
  public void toUpperCaseWithFunctionalWithPredicateExit() throws Exception {
    ItemFlowable<String, String> toUPPERCASEFlow = anItemFlow();

    StringBuffer buff = new StringBuffer();

    Subscriber<String, String> upperCaseSubscriber = toUPPERCASEFlow.publishTo(ItemSubscriber.class)
        .forEach(i -> buff.append(i.value().toUpperCase()).append(" "))
        .ifPredicateExitAndReturn(p -> p.equals("value3"), () -> buff.toString().trim())
        .subscribe();

    toUPPERCASEFlow.putItem("value1", "value2", "value3", "value4", "value5");

    toUPPERCASEFlow.close(true);

    Assert.assertThat(upperCaseSubscriber.getFutureResult().get(), is("VALUE1 VALUE2"));
  }

  @Test
  public void toUpperCaseFutureResult() throws Exception {
    FutureItemFlowable<String, String> toUPPERCASEFlow = Flow.aFutureItemFlow();

    StringBuffer buff = new StringBuffer();

    Subscriber<String, String> upperCaseSubscriber =
        toUPPERCASEFlow.publishTo(FutureItemSubscriber.class)
            .forEachReturn(i -> i.value().toUpperCase()).onEnd(() -> System.out.println("hello"))
            .subscribe();

    Assert.assertThat(toUPPERCASEFlow.submitItem("hello1").results().next().getFuture().get(),is(
        "HELLO1"));
    Assert.assertThat(toUPPERCASEFlow.submitItem("hello2").results().next().getFuture().get(),is(
        "HELLO2"));
    Assert.assertThat(toUPPERCASEFlow.submitItem("hello3").results().next().getFuture().get(),is(
        "HELLO3"));

    toUPPERCASEFlow.close(true);

  }



}
