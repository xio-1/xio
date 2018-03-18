package test;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.Flowable;
import org.xio.one.reactive.flow.core.MultiplexFutureSubscriber;
import org.xio.one.reactive.flow.core.SingleFutureSubscriber;
import org.xio.one.reactive.flow.core.SingleSubscriber;
import org.xio.one.reactive.flow.domain.Item;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AsyncStreamTest {

  public static final int NUMBER_OF_ITEMS = 1000000;
  String HELLO_WORLD_STREAM = "helloWorldStream";
  String INT_STREAM = "integerStream";


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
  public void shouldReturnHelloWorldItemFromStreamContents() {
    Flow<String, String> asyncStream = new Flow<>(HELLO_WORLD_STREAM);
    asyncStream.withImmediateFlushing().putItem("Hello world");
    asyncStream.end(true);
    assertThat(asyncStream.contents().last().value(), is("Hello world"));
  }

  @Test
  public void JSONStringReturnsHelloWorldItemFromStreamContents() throws Exception {
    Flow<String, String> asyncStream = new Flow<>(HELLO_WORLD_STREAM);
    asyncStream.withImmediateFlushing().putJSONItem("{\"msg\":\"Hello world\"}");
    asyncStream.end(true);
    assertThat(asyncStream.contents().last().jsonValue(), is("{\"msg\":\"Hello world\"}"));
  }

  @Test
  public void shouldReturnInSequenceForStreamSubscriber() throws Exception {
    Flow<Integer, List<Integer>> asyncStream = new Flow<>(INT_STREAM);

    SingleSubscriber<List<Integer>, Integer> subscriber =
        new SingleSubscriber<List<Integer>, Integer>() {

          List<Integer> output = new ArrayList<>();

          @Override
          public Optional<List<Integer>> onNext(Integer itemValue) {
            output.add(itemValue * 10);
            return Optional.empty();
          }

          @Override
          public Optional<Object> onError(Throwable error, Integer itemValue) {
            return Optional.empty();
          }

          @Override
          public void finalise() {
            this.setResult(output);
          }
        };
    asyncStream.withImmediateFlushing().addSingleSubscriber(subscriber);
    asyncStream.putItem(1, 2, 3, 4);
    asyncStream.end(true);
    Integer[] intList = new Integer[] {10, 20, 30, 40};
    Assert.assertEquals(true, Arrays.equals(subscriber.getResult().toArray(), intList));
  }

  @Test
  public void shouldReturnHelloWorldFutureForSingleFutureSubscriber() throws Exception {
    Flow<String, String> asyncStream = new Flow<>(HELLO_WORLD_STREAM);
    Future<String> result =
        asyncStream.putItem("Hello", new SingleFutureSubscriber<String, String>() {
          @Override
          public Future<String> onNext(String itemValue) {
            return CompletableFuture.completedFuture(itemValue + " world");
          }

          @Override
          public void onError(Throwable error, String itemValue) {
            error.printStackTrace();
          }
        });
    asyncStream.end(true);
    assertThat(result.get(), is("Hello world"));
  }

  @Test
  public void shouldPingAndPong() {
    Flow<String, String> ping_stream = new Flow<>("ping_stream");
    Flow<String, String> pong_stream = new Flow<>("pong_stream");

    SingleSubscriber<String, String> pingSubscriber = new SingleSubscriber<String, String>() {
      @Override
      public Optional<String> onNext(String itemValue) {
        if (itemValue.equals("ping")) {
          System.out.println("got ping");
          pong_stream.putItem("pong");
          System.out.println("sent pong");
        }
        return Optional.empty();
      }

      @Override
      public Optional<Object> onError(Throwable error, String itemValue) {
        return Optional.empty();
      }
    };

    SingleSubscriber<String, String> pongSubscriber = new SingleSubscriber<String, String>() {
      @Override
      public Optional<String> onNext(String itemValue) {
        if (itemValue.equals("pong")) {
          System.out.println("got pong");
          ping_stream.putItem("ping");
          System.out.println("sent ping");
        }
        return Optional.empty();
      }

      @Override
      public Optional<Object> onError(Throwable error, String itemValue) {
        return Optional.empty();
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
    Flow<String, Long> asyncStream = new Flow<>("sustainedPerformanceTest");
    final SingleSubscriber<Long, String> subscriber = new SingleSubscriber<Long, String>() {
      long count;

      @Override
      public void initialise() {
        this.count = 0;
      }

      @Override
      public Optional<Long> onNext(String itemValue) {
        this.count++;
        return Optional.empty();
      }

      @Override
      public Optional<Object> onError(Throwable error, String itemValue) {
        return Optional.empty();
      }

      @Override
      public void finalise() {
        this.setResult(this.count);
      }
    };
    asyncStream.addSingleSubscriber(subscriber);
    for (int i = 0; i < 10000000; i++) {
      asyncStream.putItemWithTTL(1, "Hello world" + i);
    }
    asyncStream.end(true);
    assertThat(subscriber.getResult(), is(10000000L));
    System.out
        .println("Items per second : " + 10000000 / ((System.currentTimeMillis() - start) / 1000));
  }

  @Test
  public void stability() throws Exception {
    for (int i = 0; i < 10; i++) {
      shouldReturnHelloWorldItemFromStreamContents();
      shouldReturnHelloWorldFutureForSingleFutureSubscriber();
      JSONStringReturnsHelloWorldItemFromStreamContents();
    }
  }

  @Test
  public void putForMultiplexingFutures() throws Exception {
    Flow<String, String> micro_stream = new Flow<>("micro_stream");
    MultiplexFutureSubscriber<String, String> microBatchStreamSubscriber =
        new MultiplexFutureSubscriber<String, String>() {
          @Override
          public Map<Long, String> onNext(Stream<Item<String>> e) {
            Map<Long, String> results = new HashMap<>();
            e.forEach(
                stringItem -> results.put(stringItem.itemId(), stringItem.value().toUpperCase()));
            return results;
          }
        };
    Future<String> result1 = micro_stream.putItem("hello1", microBatchStreamSubscriber);
    Future<String> result2 = micro_stream.putItem("hello2", microBatchStreamSubscriber);
    Future<String> result3 = micro_stream.putItem("hello3", microBatchStreamSubscriber);
    assertThat(result1.get(), is("HELLO1"));
    assertThat(result2.get(), is("HELLO2"));
    assertThat(result3.get(), is("HELLO3"));
  }

  @Test
  public void shouldRemoveExpiredItemsAfter1Second() throws Exception {
    Flow<String, String> asyncStream = new Flow<>("test_ttl");
    asyncStream.putItemWithTTL(10, "test10");
    asyncStream.putItemWithTTL(1, "test1");
    asyncStream.putItemWithTTL(1, "test2");
    asyncStream.putItemWithTTL(1, "test3");
    asyncStream.end(true);
    Thread.currentThread().sleep(1000);
    assertThat(asyncStream.contents().count(), is(1L));
    assertThat(asyncStream.contents().last().value(), is("test10"));
  }

  @Test
  public void shouldReturnItemUsingIndexField() {
    Flow<TestObject, TestObject> asyncStream = new Flow<>("test_index", "testField");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    asyncStream.putItem(testObject1, testObject2, testObject3);
    asyncStream.end(true);
    Assert.assertThat(asyncStream.contents().item("hello1").value(), is(testObject1));
    asyncStream.indexFieldName();
  }

  @Test
  public void shouldReturnItemUsingJSONIndexField() throws Exception {
    Flow<String, String> asyncStream = new Flow<>("test_index_json", "msg");
    asyncStream.putJSONItem("{\"msg\":\"hello1\"}");
    asyncStream.putJSONItem("{\"msg\":\"hello2\"}");
    asyncStream.putJSONItem("{\"msg\":\"hello3\"}");
    asyncStream.end(true);
    Assert.assertThat(asyncStream.contents().item("hello2").jsonValue(),
        is(("{\"msg\":\"hello2\"}")));
  }

  @Test
  public void shouldReturnItemUsingItemId() {
    Flow<TestObject, TestObject> asyncStream = new Flow<>("test_id");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    long[] itemIds = asyncStream.putItem(testObject1, testObject2, testObject3);
    asyncStream.end(true);
    Assert.assertThat(asyncStream.contents().item(itemIds[1]).value(), is(testObject2));
  }


}
