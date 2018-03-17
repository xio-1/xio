package test;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.flow.events.Event;
import org.xio.one.reactive.flow.AsyncFlow;
import org.xio.one.reactive.flow.subscribers.MultiplexFutureSubscriber;
import org.xio.one.reactive.flow.subscribers.SingleFutureSubscriber;
import org.xio.one.reactive.flow.subscribers.SingleSubscriber;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AsyncStreamTest {

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
  public void shouldReturnHelloWorldEventFromStreamContents() {
    AsyncFlow<String, String> asyncStream = new AsyncFlow<>(HELLO_WORLD_STREAM);
    asyncStream.withImmediateFlushing().putValue("Hello world");
    asyncStream.end(true);
    assertThat(asyncStream.contents().last().value(), is("Hello world"));
  }

  @Test
  public void JSONStringReturnsHelloWorldEventFromStreamContents() throws Exception {
    AsyncFlow<String, String> asyncStream = new AsyncFlow<>(HELLO_WORLD_STREAM);
    asyncStream.withImmediateFlushing().putJSONValue("{\"msg\":\"Hello world\"}");
    asyncStream.end(true);
    assertThat(asyncStream.contents().last().jsonValue(), is("{\"msg\":\"Hello world\"}"));
  }

  @Test
  public void shouldReturnInSequenceForStreamSubscriber() throws Exception {
    AsyncFlow<Integer, List<Integer>> asyncStream = new AsyncFlow<>(INT_STREAM);

    SingleSubscriber<List<Integer>, Integer> subscriber =
        new SingleSubscriber<List<Integer>, Integer>() {

          List<Integer> output = new ArrayList<>();

          @Override
          public Optional<List<Integer>> onNext(Integer eventValue) {
            output.add(eventValue * 10);
            return Optional.empty();
          }

          @Override
          public Optional<Object> onError(Throwable error, Integer eventValue) {
            return Optional.empty();
          }

          @Override
          public void finalise() {
            this.setResult(output);
          }
        };
    Future<List<Integer>> result =
        asyncStream.withImmediateFlushing().withSingleSubscriber(subscriber);
    asyncStream.putValue(1, 2, 3, 4);
    asyncStream.end(true);
    Integer[] intList = new Integer[] {10, 20, 30, 40};
    Assert
        .assertEquals(true, Arrays.equals(result.get().toArray(), intList));
  }

  @Test
  public void shouldReturnHelloWorldFutureForSingleFutureSubscriber() throws Exception {
    AsyncFlow<String, String> asyncStream = new AsyncFlow<>(HELLO_WORLD_STREAM);
    Future<String> result =
        asyncStream.putValue("Hello", new SingleFutureSubscriber<String, String>() {
          @Override
          public Future<String> onNext(String eventValue) {
            return CompletableFuture.completedFuture(eventValue + " world");
          }

          @Override
          public void onError(Throwable error, String eventValue) {
            error.printStackTrace();
          }
        });
    asyncStream.end(true);
    assertThat(result.get(), is("Hello world"));
  }

  @Test
  public void shouldPingAndPong() {
    AsyncFlow<String, String> ping_stream = new AsyncFlow<>("ping_stream");
    AsyncFlow<String, String> pong_stream = new AsyncFlow<>("pong_stream");

    SingleSubscriber<String, String> pingSubscriber = new SingleSubscriber<String, String>() {
      @Override
      public Optional<String> onNext(String eventValue) {
        if (eventValue.equals("ping")) {
          System.out.println("got ping");
          pong_stream.putValue("pong");
          System.out.println("sent pong");
        }
        return Optional.empty();
      }

      @Override
      public Optional<Object> onError(Throwable error, String eventValue) {
        return Optional.empty();
      }
    };

    SingleSubscriber<String, String> pongSubscriber = new SingleSubscriber<String, String>() {
      @Override
      public Optional<String> onNext(String eventValue) {
        if (eventValue.equals("pong")) {
          System.out.println("got pong");
          ping_stream.putValue("ping");
          System.out.println("sent ping");
        }
        return Optional.empty();
      }

      @Override
      public Optional<Object> onError(Throwable error, String eventValue) {
        return Optional.empty();
      }
    };
    ping_stream.withSingleSubscriber(pingSubscriber);
    pong_stream.withSingleSubscriber(pongSubscriber);
    ping_stream.putValue("ping");
    ping_stream.end(true);
    pong_stream.end(true);
  }

  @Test
  public void shouldSustainThroughputPerformanceTest() throws Exception {
    long start = System.currentTimeMillis();
    AsyncFlow<String, Long> asyncStream = new AsyncFlow<>("sustainedPerformanceTest");
    Future<Long> count = asyncStream.withSingleSubscriber(new SingleSubscriber<Long, String>() {
      long count;

      @Override
      public void initialise() {
        this.count = 0;
      }

      @Override
      public Optional<Long> onNext(String eventValue) {
        this.count++;
        return Optional.empty();
      }

      @Override
      public Optional<Object> onError(Throwable error, String eventValue) {
        return Optional.empty();
      }

      @Override
      public void finalise() {
        this.setResult(this.count);
      }
    });
    for (int i = 0; i < 10000000; i++) {
      asyncStream.putValueWithTTL(1, "Hello world" + i);
    }
    asyncStream.end(true);
    assertThat(count.get(), is(10000000L));
    System.out
        .println("Events per second : " + 10000000 / ((System.currentTimeMillis() - start) / 1000));
  }

  @Test
  public void stability() throws Exception {
    for (int i = 0; i < 10; i++) {
      shouldReturnHelloWorldEventFromStreamContents();
      shouldReturnHelloWorldFutureForSingleFutureSubscriber();
      JSONStringReturnsHelloWorldEventFromStreamContents();
    }
  }

  @Test
  public void putForMultiplexingFutures() throws Exception {
    AsyncFlow<String, String> micro_stream = new AsyncFlow<>("micro_stream");
    MultiplexFutureSubscriber<String, String> microBatchStreamSubscriber =
        new MultiplexFutureSubscriber<String, String>() {
          @Override
          public Map<Long, String> onNext(Stream<Event<String>> e) {
            Map<Long, String> results = new HashMap<>();
            e.forEach(stringEvent -> results
                .put(stringEvent.eventId(), stringEvent.value().toUpperCase()));
            return results;
          }
        };
    Future<String> result1 = micro_stream.putValue("hello1", microBatchStreamSubscriber);
    Future<String> result2 = micro_stream.putValue("hello2", microBatchStreamSubscriber);
    Future<String> result3 = micro_stream.putValue("hello3", microBatchStreamSubscriber);
    assertThat(result1.get(), is("HELLO1"));
    assertThat(result2.get(), is("HELLO2"));
    assertThat(result3.get(), is("HELLO3"));
  }

  @Test
  public void shouldRemoveExpiredEventsAfter1Second() throws Exception {
    AsyncFlow<String, String> asyncStream = new AsyncFlow<>("test_ttl");
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
  public void shouldReturnEventUsingIndexField() {
    AsyncFlow<TestObject, TestObject>
        asyncStream = new AsyncFlow<>("test_index", "testField");
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
    AsyncFlow<String, String> asyncStream = new AsyncFlow<>("test_index_json", "msg");
    asyncStream.putJSONValue("{\"msg\":\"hello1\"}");
    asyncStream.putJSONValue("{\"msg\":\"hello2\"}");
    asyncStream.putJSONValue("{\"msg\":\"hello3\"}");
    asyncStream.end(true);
    Assert.assertThat(asyncStream.contents().event("hello2").jsonValue(),
        is(("{\"msg\":\"hello2\"}")));
  }

  @Test
  public void shouldReturnEventUsingEventId() {
    AsyncFlow<TestObject, TestObject> asyncStream = new AsyncFlow<>("test_id");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    long[] eventIds = asyncStream.putValue(testObject1, testObject2, testObject3);
    asyncStream.end(true);
    Assert.assertThat(asyncStream.contents().event(eventIds[1]).value(), is(testObject2));
  }


}
