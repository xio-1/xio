package test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.*;
import org.xio.one.reactive.flow.subscriber.CompletableResultItemSubscriber;
import org.xio.one.reactive.flow.subscriber.FutureResultItemSubscriber;
import org.xio.one.reactive.flow.subscriber.FutureResultMultiplexItemSubscriber;
import org.xio.one.reactive.flow.subscriber.ItemSubscriber;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
    SimpleFlowable<String, String> asyncFlow = Flow.aSimpleFlowable(HELLO_WORLD_FLOW);
    asyncFlow.enableImmediateFlushing();
    asyncFlow.putItem("Hello world");
    asyncFlow.end(true);
    assertThat(asyncFlow.contents().last().value(), is("Hello world"));
  }

  @Test
  public void simpleExample() throws Exception {

    SimpleFlowable<String, String> toUPPERcaseFlow = Flow.aSimpleFlowable();

    toUPPERcaseFlow.addSubscriber(new ItemSubscriber<String, String>() {

      @Override
      public void onNext(FlowItem<String> itemValue) {
        System.out.println(itemValue.value().toUpperCase());
      }

    });

    toUPPERcaseFlow.putItem("value1", "value2");

  }

  @Test
  public void shouldReturnInSequenceForFlowSubscriber() throws Exception {
    SimpleFlowable<Integer, List<Integer>> asyncFlow = Flow.aSimpleFlowable(INT_FLOW);

    ItemSubscriber<List<Integer>, Integer> subscriber =
        new ItemSubscriber<List<Integer>, Integer>() {

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
    asyncFlow.enableImmediateFlushing();
    asyncFlow.addSubscriber(subscriber);
    asyncFlow.putItem(1, 2, 3, 4);
    asyncFlow.end(true);
    Integer[] intList = new Integer[] {10, 20, 30, 40};
    Assert.assertEquals(true, Arrays.equals(subscriber.getResult().toArray(), intList));
  }

  @Test
  public void shouldReturnHelloWorldFutureForSingleFutureSubscriber() throws Exception {
    FutureResultFlowable<String, String> asyncFlow = Flow.aFutureResultFlowable(HELLO_WORLD_FLOW,
        new FutureResultItemSubscriber<String, String>() {
          @Override
          public Future<String> onNext(String itemValue) {
            return CompletableFuture.completedFuture(itemValue + " world");
          }

          @Override
          public void onFutureCompletionError(Throwable error, String itemValue) {
            error.printStackTrace();
          }
        });
    Future<String> result = asyncFlow.submitItem("Hello");
    asyncFlow.end(true);
    assertThat(result.get(), is("Hello world"));
  }

  @Test
  public void shouldPingAndPong() {
    SimpleFlowable<String, Long> ping_stream = Flow.aSimpleFlowable("ping_stream");
    SimpleFlowable<String, Long> pong_stream = Flow.aSimpleFlowable("pong_stream");
    ping_stream.enableImmediateFlushing();
    pong_stream.enableImmediateFlushing();



    ItemSubscriber<Long, String> pingSubscriber = new ItemSubscriber<Long, String>() {

      int count = 0;

      @Override
      public void onNext(FlowItem<String> itemValue) {
        if (count < 4) {
          setResult(System.currentTimeMillis());
          System.out.println("got ping");
          pong_stream.putItem("pong");
          System.out.println("sent pong");
          count++;
        }
      }

    };

    ItemSubscriber<Long, String> pongSubscriber = new ItemSubscriber<Long, String>() {

      int count = 0;

      @Override
      public void onNext(FlowItem<String> itemValue) {
        if (count < 4) {
          setResult(System.currentTimeMillis());
          System.out.println("got pong");
          ping_stream.putItem("ping");
          System.out.println("sent ping");
          count++;
        }


      }

    };
    ping_stream.addSubscriber(pingSubscriber);
    pong_stream.addSubscriber(pongSubscriber);
    ping_stream.putItem("ping");
    ping_stream.end(true);
    pong_stream.end(true);
    System.out.println("Latency Ms = " + (pongSubscriber.getResult() - pingSubscriber.getResult()));
  }

  @Test
  public void shouldSustainThroughputPerformanceTest() throws Exception {
    long start = System.currentTimeMillis();
    SimpleFlowable<String, Long> asyncFlow = Flow.aSimpleFlowable("sustainedPerformanceTest");
    final ItemSubscriber<Long, String> subscriber = new ItemSubscriber<Long, String>() {
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
    asyncFlow.addSubscriber(subscriber);
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
      shouldHandleExceptionForFutureSubscriber();
      shouldReturnHelloWorldItemFromFlowContents();
      shouldReturnHelloWorldFutureForSingleFutureSubscriber();
    }
  }

  @Test
  public void putForMultiplexingFutures() throws Exception {
    FutureResultFlowable<String, String> micro_stream =
        Flow.aFutureResultFlowable("micro_stream", new FutureResultMultiplexItemSubscriber<>() {

          @Override
          public Map<Long, Future<String>> onNext(Stream<FlowItem<String>> e) {
            Map<Long, Future<String>> results = new HashMap<>();
            e.forEach(stringItem -> results.put(stringItem.itemId(),
                CompletableFuture.completedFuture(stringItem.value().toUpperCase())));
            return results;
          }

          @Override
          public void onFutureCompletionError(Throwable error, String itemValue) {

          }
        });

    Future<String> result1 = micro_stream.submitItem("hello1");
    Future<String> result2 = micro_stream.submitItem("hello2");
    Future<String> result3 = micro_stream.submitItem("hello3");
    assertThat(result1.get(), is("HELLO1"));
    assertThat(result2.get(), is("HELLO2"));
    assertThat(result3.get(), is("HELLO3"));
  }

  @Test
  public void shouldRemoveExpiredItemsAfter1Second() throws Exception {
    SimpleFlowable<String, String> asyncFlow = Flow.aSimpleFlowable("test_ttl");
    asyncFlow.putItemWithTTL(10, "test10");
    asyncFlow.putItemWithTTL(1, "test1");
    asyncFlow.putItemWithTTL(1, "test2");
    asyncFlow.putItemWithTTL(1, "test3");
    asyncFlow.end(true);
    Thread.currentThread().sleep(1999);
    assertThat(asyncFlow.contents().count(), is(1L));
    assertThat(asyncFlow.contents().last().value(), is("test10"));
  }

  @Ignore
  public void shouldReturnItemUsingIndexField() {
    SimpleFlowable<TestObject, TestObject> asyncFlow =
        Flow.aSimpleFlowable("test_index", "testField");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    asyncFlow.putItem(testObject1, testObject2, testObject3);
    asyncFlow.end(true);
    Assert.assertThat(asyncFlow.contents().item("hello1").value(), is(testObject1));
  }

  @Test
  public void shouldReturnItemUsingItemId() {
    SimpleFlowable<TestObject, TestObject> asyncFlow;
    asyncFlow = Flow.<TestObject, TestObject>aSimpleFlowable("test_item_id");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    long[] itemIds = asyncFlow.putItem(testObject1, testObject2, testObject3);
    asyncFlow.end(true);
    Assert.assertThat(asyncFlow.contents().item(itemIds[1]).value(), is(testObject2));
  }

  @Test
  public void shouldHandleExceptionForFutureSubscriber()
      throws ExecutionException, InterruptedException {
    FutureResultFlowable<String, String> asyncFlow = Flow.aFutureResultFlowable(HELLO_WORLD_FLOW,
        new FutureResultItemSubscriber<String, String>() {

          @Override
          public Future<String> onNext(String itemValue) {
            return CompletableFuture.supplyAsync(() -> functionalThrow(itemValue));
          }

          private String functionalThrow(String itemValue) {
            if (itemValue.equals("Hello3"))
              throw new RuntimeException("hello");
            else
              return "happy";
          }

          @Override
          public void onFutureCompletionError(Throwable error, String itemValue) {
            assert (error.getCause().getMessage().equals("hello"));
          }
        });
    Future<String> result1 = asyncFlow.submitItem("Hello1");
    Future<String> result2 = asyncFlow.submitItem("Hello2");
    Future<String> result3 = asyncFlow.submitItem("Hello3");

    if (result1.get() == null || result2.get() == null || "happy".equals(result3.get()))
      System.currentTimeMillis();
    assertThat(result1.get(), is("happy"));
    assertThat(result2.get(), is("happy"));
    assertThat(result3.get(), is(nullValue()));
    assertThat(result3.isDone(), is(true));
    asyncFlow.end(true);
  }

  @Test
  public void shouldHandleErrorForFlowSubscriber() throws Exception {

    ArrayList<String> errors = new ArrayList<>();
    SimpleFlowable<Integer, List<Integer>> asyncFlow = Flow.aSimpleFlowable(INT_FLOW);
    asyncFlow.addSubscriber(new ItemSubscriber<>() {

      int count = 0;

      @Override
      public void onNext(FlowItem<Integer> itemValue) {
        count++;
        if (count == 2)
          throw new RuntimeException("hello");
      }

      @Override
      public void onError(Throwable error, FlowItem<Integer> itemValue) {
        errors.add(error.getMessage());
      }

      @Override
      public void finalise() {
        this.setResult(new ArrayList<Integer>());
      }
    });
    asyncFlow.enableImmediateFlushing();
    asyncFlow.putItem(1, 2, 3, 4);
    asyncFlow.end(true);
    Assert.assertThat(errors.size(), is(1));
    Assert.assertThat(errors.get(0), is("hello"));
  }

  @Test
  public void testCallbackOnCompletion() throws InterruptedException {
    CompletableResultFlowable<String, String> asyncFlow =
        Flow.aCompletableResultFlowable(HELLO_WORLD_FLOW, new CompletableResultItemSubscriber<>() {

          @Override
          public void onNext(CompletableFlowItem<String, String> itemValue) throws Throwable {
            System.out.println(Thread.currentThread() + ":OnNext"+itemValue.value());
            InternalExecutors.computeThreadPoolInstance().submit(new FutureTask<Void>(() -> {
              itemValue.callback().completed(itemValue.value() + "World", itemValue.value());
              return null;
            }));
          }

          @Override
          public void onError(Throwable error, FlowItem<String> itemValue) {

          }
        });

    CompletionHandler<String, String> myHandler = new CompletionHandler<>() {
      @Override
      public void completed(String result, String attachment) {
        System.out.println(Thread.currentThread() + ":OnCallbackCompletion:"+result);

      }

      @Override
      public void failed(Throwable exc, String attachment) {
        exc.printStackTrace();
      }

    };

    System.out.println(Thread.currentThread() + ":OnSubmit");
    for (int i = 0; i < 10; i++)
      asyncFlow.submitItem("Hello" + i, myHandler);
    asyncFlow.end(true);
  }


}
