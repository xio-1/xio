package HelloWorld;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xio.one.stream.AsyncStream;
import org.xio.one.stream.reactive.SingleSubscriber;
import org.xio.one.stream.reactive.Subscribers;

import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.xio.one.stream.AsyncStreamAPI.stream;

public class HelloWorldStreamShould {

  String HELLO_WORLD = "Hello World";
  String HELLO_WORLD_ASYNC = "HelloWorldAsync";

  @Test
  public void shouldReturnHelloWorldEventFromStreamContents() throws Exception {
    stream(HELLO_WORLD,String.class, String.class).put("Hello world", true);
    stream(HELLO_WORLD).end(true);
    Assert.assertThat(stream(HELLO_WORLD).contents().getLast().getEventValue(),
        is("Hello world"));
  }

  @Test
  public void JSONStringReturnsHelloWorldEventFromStreamContents() throws Exception {
    stream(HELLO_WORLD).putJSON("{\"msg\":\"Hello world\"}", true);
    stream(HELLO_WORLD).end(true);
    Assert.assertThat(stream(HELLO_WORLD).contents().getLast().toJSONString(),
        is("{\"msg\":\"Hello world\"}"));
  }

  @Test
  public void shouldReturnHelloWorldForSingleAsyncSubscriber() throws Exception {
    AsyncStream<String,String> asyncStream = new AsyncStream<>(HELLO_WORLD_ASYNC, 0);
    Future<String> result = asyncStream.single("Hello", new SingleSubscriber<String>() {
      @Override
      public String process(String eventValue) {
        return eventValue+" world";
      }
    });
    Assert.assertThat(result.get(), is("Hello world"));
    stream(HELLO_WORLD_ASYNC).end(true);
  }

  @Test
  public void shouldReturnHelloWorldCountSubscriber() throws Exception {
    AsyncStream<String,Long> asyncStream = new AsyncStream<>("count", 0);
    Future<Long> count = asyncStream.withSubscriber(Subscribers.Counter());
    for (int i=0; i<10000;i++) {
      asyncStream.put("Hello world" +i,false);
    }
    asyncStream.end(true);
    Thread.sleep(1000);
    Assert.assertThat(count.get(),is(10000L));
  }


}
