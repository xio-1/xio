package test;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.stream.event.Event;
import org.xio.one.stream.reactive.AsyncStream;
import org.xio.one.stream.reactive.subscribers.OnEventsSubscriber;
import org.xio.one.stream.reactive.subscribers.OnNextSubscriber;
import org.xio.one.stream.reactive.subscribers.OnSingleSubscriber;

import java.util.*;
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
    AsyncStream<Integer, Object> asyncStream = new AsyncStream<>(INT_STREAM);

    OnNextSubscriber<Object, Integer> subscriber = new OnNextSubscriber<Object, Integer>() {

      List<Integer> output = new ArrayList<>();

      @Override
      public Boolean onNext(Integer eventValue) {
        output.add(eventValue * 10);
        return true;
      }

      @Override
      public Object onError(Throwable error, Integer eventValue) {
        return false;
      }

      @Override
      public void finalise() {
        this.setResult(output);
      }
    };
    Future<Object> result = asyncStream.withImmediateFlushing().withStreamSubscriber(subscriber);
    asyncStream.putValue(1, 2, 3, 4);
    asyncStream.end(true);
    Integer[] intList = new Integer[] {10, 20, 30, 40};
    Assert
        .assertEquals(true, Arrays.equals(((ArrayList<Integer>) result.get()).toArray(), intList));
  }

  @Test
  public void shouldReturnHelloWorldForSingleAsyncSubscriber() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD_STREAM);
    Future<String> result =
        asyncStream.putValueToSingleSubscriber("Hello", new OnSingleSubscriber<String, String>() {

          @Override
          public String onSingle(String eventValue) {
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

    OnSingleSubscriber<String, String> pingSubscriber = new OnSingleSubscriber<String, String>() {
      @Override
      public String onSingle(String eventValue) {
        if (eventValue.equals("ping")) {
          System.out.println("got ping");
          pong_stream.putValue("pong");
          System.out.println("sent pong");
        }
        return "";
      }
    };

    OnSingleSubscriber<String, String> pongSubscriber = new OnSingleSubscriber<String, String>() {
      @Override
      public String onSingle(String eventValue) {
        if (eventValue.equals("pong")) {
          System.out.println("got pong");
          ping_stream.putValue("ping");
          System.out.println("sent ping");
        }
        return "";
      }
    };
    ping_stream.withStreamSubscriber(pingSubscriber);
    pong_stream.withStreamSubscriber(pongSubscriber);
    ping_stream.putValue("ping");
    ping_stream.end(true);
    pong_stream.end(true);
  }

  @Test
  public void shouldPerformance() throws Exception {
    long start = System.currentTimeMillis();
    AsyncStream<String, Long> asyncStream = new AsyncStream<>("count");
    Future<Long> count = asyncStream.withStreamSubscriber(new OnNextSubscriber<Long, String>() {
      long count;

      @Override
      public void initialise() {
        this.count = 0;
      }

      @Override
      public Long onNext(String eventValue) {
        this.count++;
        return count;
      }

      @Override
      public Object onError(Throwable error, String eventValue) {
        return null;
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
    System.out
        .println("Events per second : " + 1000000 / ((System.currentTimeMillis() - start) / 1000));
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
    OnEventsSubscriber<String, String> microBatchStreamSubscriber =
        new OnEventsSubscriber<String, String>() {
          @Override
          public Map<Long, String> onEvents(Stream<Event<String>> e) {
            Map<Long, String> results = new HashMap<>();
            e.forEach(stringEvent -> results
                .put(stringEvent.eventId(), stringEvent.value().toUpperCase()));
            return results;
          }

          @Override
          public void initialise() {
          }
        };
    Future<String> result =
        micro_stream.putValueToMicroBatchSubscriber("hello", microBatchStreamSubscriber);
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
    Assert.assertThat(asyncStream.contents().event("hello2").jsonValue(),
        is(("{\"msg\":\"hello2\"}")));
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
