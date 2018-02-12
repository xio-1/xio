package HelloWorld;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.stream.AsyncStream;
import org.xio.one.stream.reactive.SingleEventSubscriber;
import org.xio.one.stream.reactive.OnlyNextEventStreamSubscriber;
import org.xio.one.stream.reactive.ContinuousStreamSubscriber;
import org.xio.one.stream.reactive.Subscribers;

import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;

public class AsyncStreamTestsShould {

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

    ContinuousStreamSubscriber<Integer> subscriber =
        new ContinuousStreamSubscriber<Integer>() {
          @Override
          public Integer process(Integer eventValue) {
            return eventValue * 10;
          }
        };
    Future<Stream<Integer>> result = asyncStream.withImmediateFlushing().withSubscriber(subscriber);
    asyncStream.put(1, 2, 3, 4);
    asyncStream.end(true);
    Integer[] intList = new Integer[] {10, 20, 30, 40};
    Assert.assertTrue(
        Arrays.equals(result.get().toArray(Integer[]::new), intList));
  }

  @Test
  public void shouldReturnHelloWorldForSingleAsyncSubscriber() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD_STREAM, 0);
    Future<String> result =
        asyncStream.just(
            "Hello",
            new SingleEventSubscriber<String>() {

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
    Future<Long> count = asyncStream.withSubscriber(Subscribers.Counter());
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

    OnlyNextEventStreamSubscriber<String>
        pingSubscriber = new OnlyNextEventStreamSubscriber<String>() {
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

    OnlyNextEventStreamSubscriber<String>
        pongSubscriber = new OnlyNextEventStreamSubscriber<String>() {
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
  public void shouldPerformance() throws Exception {
    long start = System.currentTimeMillis();
    AsyncStream<String, Long> asyncStream = new AsyncStream<>("count", 0);
    Future<Long> count = asyncStream.withSubscriber(Subscribers.Counter());
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
