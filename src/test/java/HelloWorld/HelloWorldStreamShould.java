package HelloWorld;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.stream.AsyncStream;
import org.xio.one.stream.reactive.SingleSubscriber;
import org.xio.one.stream.reactive.Subscribers;

import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;

public class HelloWorldStreamShould {

  String HELLO_WORLD = "Hello World";

  @Test
  public void shouldReturnHelloWorldEventFromStreamContents() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD, 0);
    asyncStream.put("Hello world", true);
    asyncStream.end(true);
    Assert.assertThat(asyncStream.contents().getLast().getEventValue(), is("Hello world"));
  }

  @Test
  public void JSONStringReturnsHelloWorldEventFromStreamContents() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD, 0);
    asyncStream.putJSON("{\"msg\":\"Hello world\"}", true);
    asyncStream.end(true);
    Assert.assertThat(
        asyncStream.contents().getLast().toJSONString(), is("{\"msg\":\"Hello world\"}"));
  }

  @Test
  public void shouldReturnHelloWorldForSingleAsyncSubscriber() throws Exception {
    AsyncStream<String, String> asyncStream = new AsyncStream<>(HELLO_WORLD, 0);
    Future<String> result =
        asyncStream.single(
            "Hello",
            new SingleSubscriber<String>() {
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
      asyncStream.put("Hello world" + i, false);
    }
    asyncStream.end(true);
    Assert.assertThat(count.get(), is(10000L));
  }

  @Test
  public void shouldPerformance() throws Exception {
    long start = System.currentTimeMillis();
    AsyncStream<String, Long> asyncStream = new AsyncStream<>("count", 0);
    Future<Long> count = asyncStream.withSubscriber(Subscribers.Counter());
    for (int i = 0; i < 1000000; i++) {
      asyncStream.put("Hello world" + i, false);
    }
    asyncStream.end(true);
    Assert.assertThat(count.get(), is(1000000L));
    System.out.println("Events per second : " + 1000000/((System.currentTimeMillis()-start)/1000));
  }

  @Test
  public void stability() throws Exception {
    for (int i = 0; i < 100; i++) {
      shouldReturnHelloWorldCountSubscriber();
      shouldReturnHelloWorldEventFromStreamContents();
      shouldReturnHelloWorldForSingleAsyncSubscriber();
      JSONStringReturnsHelloWorldEventFromStreamContents();
    }
  }
}
