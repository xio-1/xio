package test;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.stream.AsyncStream;
import org.xio.one.stream.event.Event;
import org.xio.one.stream.reactive.AsyncStreamExecutor;
import org.xio.one.stream.reactive.subscribers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;

public class AsyncStreamTestsShould {

  public static final int NUMBER_OF_EVENTS = 1000000;
  String HELLO_WORLD_STREAM = "helloWorldStream";
  String INT_STREAM = "integerStream";

  @Test
  public void shouldReturnHelloWorldEventFromStreamContents() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD_STREAM, 0);
    asyncStream.withImmediateFlushing().put("Hello world");
    asyncStream.end(true);
    Assert.assertThat(asyncStream.contents().getLast().getEventValue(), is("Hello world"));
  }

  @Test
  public void JSONStringReturnsHelloWorldEventFromStreamContents() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD_STREAM, 0);
    asyncStream.withImmediateFlushing().putJSON("{\"msg\":\"Hello world\"}");
    asyncStream.end(true);
    Assert.assertThat(
        asyncStream.contents().getLast().toJSONString(), is("{\"msg\":\"Hello world\"}"));
  }

  @Test
  public void shouldReturnInSequenceForStreamSubscriber() throws Exception {
    AsyncStream<Integer, Integer> asyncStream = new AsyncStream<>(INT_STREAM, 0);
    ContinuousCollectingStreamSubscriber<Integer> subscriber =
        new ContinuousCollectingStreamSubscriber<Integer>() {
          @Override
          public Integer process(Integer eventValue) {
            return eventValue * 10;
          }
        };
    Future<Stream<Integer>> result = asyncStream.withImmediateFlushing().withSubscriber(subscriber);
    asyncStream.put(1, 2, 3, 4);
    asyncStream.end(true);
    Integer[] intList = new Integer[] {10, 20, 30, 40};
    Assert.assertTrue(Arrays.equals(result.get().toArray(Integer[]::new), intList));
  }

  @Test
  public void shouldReturnHelloWorldForSingleAsyncSubscriber() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD_STREAM, 0);
    Future<String> result =
        asyncStream.just(
            "Hello",
            new JustOneEventSubscriber<String>() {

              @Override
              public String process(String eventValue) {
                return eventValue + " world";
              }
            });
    asyncStream.end(true);
    Assert.assertThat(result.get(), is("Hello world"));
  }

  @Test
  public void shouldReturnHelloWorldCountSubscriber() throws Exception {
    AsyncStream<String, Long> asyncStream = new AsyncStream<>("count", 0);
    Future<Long> count = asyncStream.withSubscriber(new ContinuousCountingStreamSubscriber());
    for (int i = 0; i < 10000; i++) {
      asyncStream.put("Hello world" + i);
    }
    asyncStream.end(true);
    Assert.assertThat(count.get(), is(10000L));
  }

  @Test
  public void shouldPingAndPong() throws Exception {
    AsyncStream<String, String> ping_stream = new AsyncStream<>("ping_stream", 0);
    AsyncStream<String, String> pong_stream = new AsyncStream<>("pong_stream", 0);

    NextSingleEventSubscriber<String> pingSubscriber =
        new NextSingleEventSubscriber<String>() {
          @Override
          public String process(String eventValue) {
            if (eventValue.equals("ping")) {
              System.out.println("got ping");
              pong_stream.put("pong");
              System.out.println("sent pong");
            }
            return "";
          }
        };

    NextSingleEventSubscriber<String> pongSubscriber =
        new NextSingleEventSubscriber<String>() {
          @Override
          public String process(String eventValue) {
            if (eventValue.equals("pong")) {
              System.out.println("got pong");
              ping_stream.put("ping");
              System.out.println("sent ping");
            }
            return "";
          }
        };
    ping_stream.withSubscriber(pingSubscriber);
    pong_stream.withSubscriber(pongSubscriber);
    ping_stream.put("ping");
    ping_stream.end(true);
    pong_stream.end(true);
  }

  @Test
  public void shouldProcessMicroBatch() throws Exception {

    AsyncStream<String, List<String>> micro_stream = new AsyncStream<>("micro_stream", 0);

    NextMicroBatchStreamSubscriber<List<String>> microBatchEventProcessor =
        new NextMicroBatchStreamSubscriber<List<String>>() {

          List<String> microBatchOutput;

          @Override
          public void initialise() {
            microBatchOutput = new ArrayList<>();
          }

          @Override
          protected List<String> processStream(Stream<Event> e) {
            e.forEach(
                event -> microBatchOutput.add(event.getEventValue().toString().toUpperCase()));
            return microBatchOutput;
          }
        };

    AsyncStreamExecutor.subscriberCachedThreadPoolInstance()
        .submit(
            new Thread(
                () -> {
                  for (int i = 0; i < NUMBER_OF_EVENTS; i++) {
                    micro_stream.put("Hello world: " + i);
                  }
                  micro_stream.end(true);
                }));

    int count = 0;
    while (!micro_stream.hasEnded())
      count = count + micro_stream.withSubscriber(microBatchEventProcessor).get().size();
    Assert.assertThat(count, is(NUMBER_OF_EVENTS));
  }

  @Test
  public void shouldPerformance() throws Exception {
    long start = System.currentTimeMillis();
    AsyncStream<String, Long> asyncStream = new AsyncStream<>("count", 0);
    Future<Long> count = asyncStream.withSubscriber(new ContinuousCountingStreamSubscriber());
    for (int i = 0; i < 1000000; i++) {
      asyncStream.put("Hello world" + i);
    }
    asyncStream.end(true);
    Assert.assertThat(count.get(), is(1000000L));
    System.out.println(
        "Events per second : " + 1000000 / ((System.currentTimeMillis() - start) / 1000));
  }

  @Test
  public void stability() throws Exception {
    for (int i = 0; i < 10; i++) {
      shouldReturnHelloWorldCountSubscriber();
      shouldReturnHelloWorldEventFromStreamContents();
      shouldReturnHelloWorldForSingleAsyncSubscriber();
      JSONStringReturnsHelloWorldEventFromStreamContents();
    }
  }
}
