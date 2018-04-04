package test;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.Flowable;
import org.xio.one.reactive.flow.core.FlowItemSubscriber;
import org.xio.one.reactive.flow.core.FutureFlowItemMultiplexSubscriber;
import org.xio.one.reactive.flow.core.FutureFlowItemSubscriber;
import org.xio.one.reactive.flow.core.domain.FlowItem;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FlowTest {

  public static final int NUMBER_OF_ITEMS = 1000000;
  String HELLO_WORLD_FLOW = "helloWorldFlow";
  String INT_FLOW = "integerFlow";


  public class TestObject {
    String testField;

    public TestObject(String testField) {
      this.testField = testField;
    }

    public String getTestField() {
      return testField;
    }
  }

  @Test
  public void shouldReturnHelloWorldItemFromFlowContents() {
    Flowable<String, String> asyncFlow = Flow.aFlowable(HELLO_WORLD_FLOW);
    asyncFlow.enableImmediateFlushing().putItem("Hello world");
    asyncFlow.end(true);
    assertThat(asyncFlow.contents().last().value(), is("Hello world"));
  }

  @Test
  public void JSONStringReturnsHelloWorldItemFromFlowContents() throws Exception {
    Flowable<String, String> asyncFlow = Flow.aFlowable(HELLO_WORLD_FLOW);
    asyncFlow.enableImmediateFlushing().putJSONItem("{\"msg\":\"Hello world\"}");
    asyncFlow.end(true);
    assertThat(asyncFlow.contents().last().jsonValue(), is("{\"msg\":\"Hello world\"}"));
  }

  @Test
  public void simpleExample() throws Exception {

    Flowable<String, String> toUPPERcaseFlow = Flow.aFlowable();

    toUPPERcaseFlow.addSingleSubscriber(new FlowItemSubscriber<String, String>() {

      @Override
      public void onNext(FlowItem<String> itemValue) {
        System.out.println(itemValue.value().toUpperCase());
      }

    });

    toUPPERcaseFlow.putItem("value1", "value2");

  }

  @Test
  public void shouldReturnInSequenceForFlowSubscriber() throws Exception {
    Flowable<Integer, List<Integer>> asyncFlow = Flow.aFlowable(INT_FLOW);

    FlowItemSubscriber<List<Integer>, Integer> subscriber =
        new FlowItemSubscriber<List<Integer>, Integer>() {

          List<Integer> output = new ArrayList<>();

          @Override
          public void onNext(FlowItem<Integer> itemValue) {
            output.add(itemValue.value() * 10);
          }

          @Override
          public void finalise() {
            this.setResult(output);
          }
        };
    asyncFlow.enableImmediateFlushing().addSingleSubscriber(subscriber);
    asyncFlow.putItem(1, 2, 3, 4);
    asyncFlow.end(true);
    Integer[] intList = new Integer[] {10, 20, 30, 40};
    Assert.assertEquals(true, Arrays.equals(subscriber.getResult().toArray(), intList));
  }

  @Test
  public void shouldReturnHelloWorldFutureForSingleFutureSubscriber() throws Exception {
    Flowable<String, String> asyncFlow = Flow.aFlowable(HELLO_WORLD_FLOW);
    Future<String> result =
        asyncFlow.putItem("Hello", new FutureFlowItemSubscriber<String, String>() {
          @Override
          public Future<String> onNext(String itemValue) {
            return CompletableFuture.completedFuture(itemValue + " world");
          }

          @Override
          public void onFutureError(Throwable error, String itemValue) {
            error.printStackTrace();
          }
        });
    asyncFlow.end(true);
    assertThat(result.get(), is("Hello world"));
  }

  @Test
  public void shouldPingAndPong() {
    Flowable<String, String> ping_stream = Flow.aFlowable("ping_stream");
    Flowable<String, String> pong_stream = Flow.aFlowable("pomg_stream");
    ping_stream.enableImmediateFlushing();
    pong_stream.enableImmediateFlushing();
    FlowItemSubscriber<String, String> pingSubscriber = new FlowItemSubscriber<String, String>() {
      @Override
      public void onNext(FlowItem<String> itemValue) {
        if (itemValue.value().equals("ping")) {
          System.out.println("got ping");
          pong_stream.putItem("pong");
          System.out.println("sent pong");
        }
      }
    };

    FlowItemSubscriber<String, String> pongSubscriber = new FlowItemSubscriber<String, String>() {
      @Override
      public void onNext(FlowItem<String> itemValue) {
        if (itemValue.value().equals("pong")) {
          System.out.println("got pong");
          ping_stream.putItem("ping");
          System.out.println("sent ping");
        }
      }

    };
    ping_stream.addSingleSubscriber(pingSubscriber);
    pong_stream.addSingleSubscriber(pongSubscriber);
    ping_stream.putItem("ping");
    ping_stream.end(true);
    pong_stream.end(true);
  }

  @Test
  public void shouldSustainThroughputPerformanceTest() throws Exception {
    long start = System.currentTimeMillis();
    Flowable<String, Long> asyncFlow = Flow.aFlowable("sustainedPerformanceTest");
    final FlowItemSubscriber<Long, String> subscriber = new FlowItemSubscriber<Long, String>() {
      long count;

      @Override
      public void initialise() {
        this.count = 0;
      }

      @Override
      public void onNext(FlowItem<String> itemValue) {
        this.count++;
      }

      @Override
      public void finalise() {
        this.setResult(this.count);
      }
    };
    asyncFlow.addSingleSubscriber(subscriber);
    for (int i = 0; i < 10000000; i++) {
      asyncFlow.putItemWithTTL(1, "Hello world" + i);
    }
    asyncFlow.end(true);
    assertThat(subscriber.getResult(), is(10000000L));
    System.out
        .println("Items per second : " + 10000000 / ((System.currentTimeMillis() - start) / 1000));
  }

  @Test
  public void stability() throws Exception {
    for (int i = 0; i < 10; i++) {
      shouldReturnHelloWorldItemFromFlowContents();
      shouldReturnHelloWorldFutureForSingleFutureSubscriber();
      JSONStringReturnsHelloWorldItemFromFlowContents();
    }
  }

  @Test
  public void putForMultiplexingFutures() throws Exception {
    Flowable<String, String> micro_stream = Flow.aFlowable("micro_stream");
    FutureFlowItemMultiplexSubscriber<String, String> microBatchFlowSubscriber =
        new FutureFlowItemMultiplexSubscriber<String, String>() {

          @Override
          public Map<Long, Future<String>> onNext(Stream<FlowItem<String>> e) {
            Map<Long, Future<String>> results = new HashMap<>();
            e.forEach(stringItem -> results.put(stringItem.itemId(),
                CompletableFuture.completedFuture(stringItem.value().toUpperCase())));
            return results;
          }

          @Override
          public void onFutureError(Throwable error, String itemValue) {

          }
        };
    Future<String> result1 = micro_stream.putItem("hello1", microBatchFlowSubscriber);
    Future<String> result2 = micro_stream.putItem("hello2", microBatchFlowSubscriber);
    Future<String> result3 = micro_stream.putItem("hello3", microBatchFlowSubscriber);
    assertThat(result1.get(), is("HELLO1"));
    assertThat(result2.get(), is("HELLO2"));
    assertThat(result3.get(), is("HELLO3"));
  }

  @Test
  public void shouldRemoveExpiredItemsAfter1Second() throws Exception {
    Flowable<String, String> asyncFlow = Flow.aFlowable("test_ttl");
    asyncFlow.putItemWithTTL(10, "test10");
    asyncFlow.putItemWithTTL(1, "test1");
    asyncFlow.putItemWithTTL(1, "test2");
    asyncFlow.putItemWithTTL(1, "test3");
    asyncFlow.end(true);
    Thread.currentThread().sleep(1000);
    assertThat(asyncFlow.contents().count(), is(1L));
    assertThat(asyncFlow.contents().last().value(), is("test10"));
  }

  @Test
  public void shouldReturnItemUsingIndexField() {
    Flowable<TestObject, TestObject> asyncFlow = Flow.aFlowable("test_index", "testField");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    asyncFlow.putItem(testObject1, testObject2, testObject3);
    asyncFlow.end(true);
    Assert.assertThat(asyncFlow.contents().item("hello1").value(), is(testObject1));
  }

  @Test
  public void shouldReturnItemUsingJSONIndexField() throws Exception {
    Flowable<String, String> asyncFlow = Flow.aFlowable("test_index_json", "msg");
    asyncFlow.putJSONItem("{\"msg\":\"hello1\"}");
    asyncFlow.putJSONItem("{\"msg\":\"hello2\"}");
    asyncFlow.putJSONItem("{\"msg\":\"hello3\"}");
    asyncFlow.end(true);
    Assert
        .assertThat(asyncFlow.contents().item("hello2").jsonValue(), is(("{\"msg\":\"hello2\"}")));
  }

  @Test
  public void shouldReturnItemUsingItemId() {
    Flowable<TestObject, TestObject> asyncFlow;
    asyncFlow = Flow.<TestObject, TestObject>aFlowable("test_item_id");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    long[] itemIds = asyncFlow.putItem(testObject1, testObject2, testObject3);
    asyncFlow.end(true);
    Assert.assertThat(asyncFlow.contents().item(itemIds[1]).value(), is(testObject2));
  }

}
