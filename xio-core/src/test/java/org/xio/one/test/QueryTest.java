package org.xio.one.test;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.item.Item;

import static org.hamcrest.core.Is.is;

public class QueryTest {

  @Test
  public void shouldReturnSize0WhenEmpty() throws InterruptedException {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    Thread.sleep(100);
    Assert.assertThat(flowable.contents().size(), is(0L));
  }

  @Test
  public void shouldReturnLastItem() throws InterruptedException {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    flowable.putItem(1,2,3);
    Thread.sleep(100);
    Item<Integer>  item = flowable.contents().last();
    Assert.assertThat(item.value(), is(3));
  }

  @Test
  public void shouldReturnFirstItem() throws InterruptedException {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    flowable.putItem(1,2,3);
    Thread.sleep(100);
    Item<Integer>  item = flowable.contents().first();
    Assert.assertThat(item.value(), is(1));
  }

  @Test
  public void shouldReturnAllItems() throws InterruptedException {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    flowable.putItem(1,2,3);
    Thread.sleep(100);
    Item<Integer>[]  items = flowable.contents().allItems();
    Assert.assertThat(items[0].value(), is(1));
    Assert.assertThat(items[1].value(), is(2));
    Assert.assertThat(items[2].value(), is(3));
  }

  @Test
  public void shouldReturnConsistentSnapshot() {
    ItemFlowable<Integer,Integer> flowable = Flow.anItemFlow();
    flowable.putItem(1,2,3);
    Assert.assertThat(flowable.snapshot().length,is(3));
  }



}
