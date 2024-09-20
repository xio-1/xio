package org.xio.one.test;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.item.Item;

import static org.hamcrest.core.Is.is;

public class FlowQueryTest {

  @BeforeClass
  public static void setup() {
    XIOService.start();
  }

  @AfterClass
  public static void tearDown() {
    XIOService.stop();
  }


  @Test
  public void shouldReturnSize0WhenEmpty() throws InterruptedException {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    flowable.close(true);
    Assert.assertThat(flowable.getSink().size(), is(0L));
  }

  @Test
  public void shouldReturnLastItem() throws InterruptedException {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    flowable.putItem(1,2,3);
    flowable.close(true);
    Item<Integer>  item = flowable.getSink().lastItem();
    Assert.assertThat(item.value(), is(3));
  }

  @Test
  public void shouldReturnFirstItem() throws InterruptedException {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    flowable.putItem(1,2,3);
    flowable.close(true);
    Item<Integer>  item = flowable.getSink().firstItem();
    Assert.assertThat(item.value(), is(1));
  }

  @Test
  public void shouldReturnAllItems() throws InterruptedException {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    flowable.putItem(1,2,3);
    flowable.close(true);
    Item<Integer>[]  items = flowable.getSink().allItems();
    Assert.assertThat(items[0].value(), is(1));
    Assert.assertThat(items[1].value(), is(2));
    Assert.assertThat(items[2].value(), is(3));
  }

  @Test
  public void shouldReturnConsistentSnapshot() {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    flowable.putItem(1,2,3);
    flowable.close(true);
    Assert.assertThat(flowable.takeSinkSnapshot().length,is(3));
  }



}
