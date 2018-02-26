package test;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.stream.event.Event;
import org.xio.one.stream.reactive.AsyncStream;
import org.xio.one.stream.reactive.subscribers.CollectingSubscriber;
import org.xio.one.stream.reactive.subscribers.FutureMicroBatchSubscriber;
import org.xio.one.stream.reactive.subscribers.FutureSingleSubscriber;
import org.xio.one.stream.reactive.subscribers.StreamSubscriber;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AsyncStreamTestsShould {

  public static final int NUMBER_OF_EVENTS = 1000000;
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
  public void shouldReturnHelloWorldEventFromStreamContents() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD_STREAM);
    asyncStream.withImmediateFlushing().putValue("Hello world");
    asyncStream.end(true);
    assertThat(asyncStream.contents().last().value(), is("Hello world"));
  }

  @Test
  public void JSONStringReturnsHelloWorldEventFromStreamContents() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD_STREAM);
    asyncStream.withImmediateFlushing().putJSONValue("{\"msg\":\"Hello world\"}");
    asyncStream.end(true);
    assertThat(asyncStream.contents().last().jsonValue(), is("{\"msg\":\"Hello world\"}"));
  }

  @Test
  public void shouldReturnInSequenceForStreamSubscriber() throws Exception {
    AsyncStream<Integer, List<Integer>> asyncStream = new AsyncStream<>(INT_STREAM);
    CollectingSubscriber<Integer, Integer> subscriber =
        new CollectingSubscriber<Integer, Integer>() {
          @Override
          public Integer process(Integer eventValue) {
            return eventValue * 10;
          }
        };
    Future<List<Integer>> result = asyncStream.withImmediateFlushing().withSubscriber(subscriber);
    asyncStream.putValue(1, 2, 3, 4);
    asyncStream.end(true);
    Integer[] intList = new Integer[] {10, 20, 30, 40};
    Assert.assertTrue(Arrays.equals(result.get().toArray(new Integer[4]), intList));
  }

  @Test
  public void shouldReturnHelloWorldForSingleAsyncSubscriber() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD_STREAM);
    Future<String> result =
        asyncStream.putValueForSingleSubscriber(
            "Hello",
            new FutureSingleSubscriber<String, String>() {

              @Override
              public String process(String eventValue) {
                return eventValue + " world";
              }
            });
    asyncStream.end(true);
    assertThat(result.get(), is("Hello world"));
  }

  @Test
  public void shouldPingAndPong() throws Exception {
    AsyncStream<String, String> ping_stream = new AsyncStream<>("ping_stream");
    AsyncStream<String, String> pong_stream = new AsyncStream<>("pong_stream");

    FutureSingleSubscriber<String, String> pingSubscriber =
        new FutureSingleSubscriber<String, String>() {
          @Override
          public String process(String eventValue) {
            if (eventValue.equals("ping")) {
              System.out.println("got ping");
              pong_stream.putValue("pong");
              System.out.println("sent pong");
            }
            return "";
          }
        };

    FutureSingleSubscriber<String, String> pongSubscriber =
        new FutureSingleSubscriber<String, String>() {
          @Override
          public String process(String eventValue) {
            if (eventValue.equals("pong")) {
              System.out.println("got pong");
              ping_stream.putValue("ping");
              System.out.println("sent ping");
            }
            return "";
          }
        };
    ping_stream.withSubscriber(pingSubscriber);
    pong_stream.withSubscriber(pongSubscriber);
    ping_stream.putValue("ping");
    ping_stream.end(true);
    pong_stream.end(true);
  }

  @Test
  public void shouldPerformance() throws Exception {
    long start = System.currentTimeMillis();
    AsyncStream<String, Long> asyncStream = new AsyncStream<>("count");
    Future<Long> count =
        asyncStream.withSubscriber(
            new StreamSubscriber<Long, String>() {
              long count;

              @Override
              public void initialise() {
                this.count = 0;
              }

              @Override
              public Long process(String eventValue) {
                this.count++;
                return this.count;
              }

              @Override
              public void finalise() {
                this.setResult(this.count);
              }
            });
    for (int i = 0; i < 1000000; i++) {
      asyncStream.putValue("Hello world" + i);
    }
    asyncStream.end(true);
    assertThat(count.get(), is(1000000L));
    System.out.println(
        "Events per second : " + 1000000 / ((System.currentTimeMillis() - start) / 1000));
  }

  @Test
  public void stability() throws Exception {
    for (int i = 0; i < 10; i++) {
      shouldReturnHelloWorldEventFromStreamContents();
      shouldReturnHelloWorldForSingleAsyncSubscriber();
      JSONStringReturnsHelloWorldEventFromStreamContents();
    }
  }

  @Test
  public void putForMicroBatching() throws Exception {
    AsyncStream<String, String> micro_stream = new AsyncStream<>("micro_stream");
    FutureMicroBatchSubscriber<String, String> microBatchStreamSubscriber =
        new FutureMicroBatchSubscriber<String, String>() {
          @Override
          public Map<Long, String> processStream(Stream<Event<String>> e) {
            Map<Long, String> results = new HashMap<>();
            e.forEach(
                stringEvent ->
                    results.put(stringEvent.eventId(), stringEvent.value().toUpperCase()));
            return results;
          }

          @Override
          public void initialise() {}
        };
    Future<String> result =
        micro_stream.putValueForMicroBatchSubscriber("hello", microBatchStreamSubscriber);
    assertThat(result.get(), is("HELLO"));
  }

  @Test
  public void shouldRemoveExpiredEventsAfter1Second() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>("test_ttl");
    asyncStream.putValueWithTTL(10, "test10");
    asyncStream.putValueWithTTL(1, "test1");
    asyncStream.putValueWithTTL(1, "test2");
    asyncStream.putValueWithTTL(1, "test3");
    asyncStream.end(true);
    Thread.currentThread().sleep(1000);
    assertThat(asyncStream.contents().count(), is(1L));
    assertThat(asyncStream.contents().last().value(), is("test10"));
  }

  @Test
  public void shouldReturnEventUsingIndexField() throws Exception {
    AsyncStream<TestObject, TestObject> asyncStream = new AsyncStream<>("test_index", "testField");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    asyncStream.putValue(testObject1, testObject2, testObject3);
    asyncStream.end(true);
    Assert.assertThat(asyncStream.contents().event("hello1").value(), is(testObject1));
    asyncStream.indexFieldName();
  }

  @Test
  public void shouldReturnEventUsingJSONIndexField() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>("test_index_json", "msg");
    asyncStream.putJSONValue("{\"msg\":\"hello1\"}");
    asyncStream.putJSONValue("{\"msg\":\"hello2\"}");
    asyncStream.putJSONValue("{\"msg\":\"hello3\"}");
    asyncStream.end(true);
    Assert.assertThat(
        asyncStream.contents().event("hello2").jsonValue(), is(("{\"msg\":\"hello2\"}")));
  }

  @Test
  public void shouldReturnEventUsingEventId() throws Exception {
    AsyncStream<TestObject, TestObject> asyncStream = new AsyncStream<>("test_id");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    long[] eventIds = asyncStream.putValue(testObject1, testObject2, testObject3);
    asyncStream.end(true);
    Assert.assertThat(asyncStream.contents().event(eventIds[1]).value(), is(testObject2));
  }
}
