package org.xio.one.test;

import org.junit.*;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.Promise;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.domain.flow.CompletableItemFlowable;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.flow.FutureItemFlowable;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.CompletableItemSubscriber;
import org.xio.one.reactive.flow.subscribers.FutureItemSubscriber;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.xio.one.reactive.flow.Flow.anItemFlow;

public class FlowTest {

  public static final int NUMBER_OF_ITEMS = 1000000;
  final Logger logger = Logger.getLogger(FlowTest.class.getCanonicalName());
  String HELLO_WORLD_FLOW = "helloWorldFlow";
  String INT_FLOW = "integerFlow";

  public static Map<String, Long> countWords(Stream<String> names) {
    return names.collect(groupingBy(name -> name, counting()));
  }

  @BeforeClass
  public static void setup() {
    XIOService.start();
  }

  @AfterClass
  public static void tearDown() {
    XIOService.stop();
  }

  @Test
  public void HelloWorld() {
    ItemFlowable<String, String> asyncFlow = anItemFlow("HelloWorldFlow");
    asyncFlow.publishTo(ItemSubscriber.class).forEach(i -> logger.info(i.value())).subscribe();
    asyncFlow.putItem("Hello World!!!");
    asyncFlow.close(true);
  }

  @Test
  public void shouldReturnHelloWorldItemFromFlowContents() throws InterruptedException {
    ItemFlowable<String, String> asyncFlow = anItemFlow(HELLO_WORLD_FLOW);
    asyncFlow.enableImmediateFlushing();
    asyncFlow.putItem("Hello world");
    Thread.sleep(100);
    assertThat(asyncFlow.contents().last().value().toString(), is("Hello world"));
    asyncFlow.close(true);

  }

  @Test
  public void toUpperCaseSubscriberExample() throws Exception {
    ItemFlowable<String, String> toUPPERCASEFlow = anItemFlow();
    Subscriber<String, String> upperCaseSubscriber =
        toUPPERCASEFlow.addSubscriber(new ItemSubscriber<>() {

          StringBuffer stringBuffer;

          @Override
          public void initialise() {
            logger.info("initialising");
            stringBuffer = new StringBuffer();
          }

          @Override
          public void onNext(Item<String, String> itemValue) {
            logger.info("on next " + itemValue.value());
            stringBuffer.append(itemValue.value().toUpperCase());
            stringBuffer.append(" ");
          }

          @Override
          public String finalise() {
            logger.info("finalising");
            return stringBuffer.toString().trim();
          }
        });
    toUPPERCASEFlow.putItem("value1", "value2", "value3", "value4", "value5");

    toUPPERCASEFlow.close(true);

    Assert.assertThat(upperCaseSubscriber.getFutureResult().get(),
        is("VALUE1 VALUE2 VALUE3 VALUE4 VALUE5"));
  }



  @Test
  public void shouldCountWords() {
    countWords(Arrays.stream("The quick quick brown fox".split(" ")))
        .forEach((key, value) -> logger.info(key + "," + value));
  }


  @Test
  public void shouldReturnInSequenceForFlowSubscriber() throws Exception {

    ItemFlowable<Integer, List<Integer>> asyncFlow = anItemFlow(INT_FLOW);

    ItemSubscriber<List<Integer>, Integer> subscriber = new ItemSubscriber<>() {

      private List<Integer> output = new ArrayList<>();

      @Override
      public void onNext(Item<Integer, List<Integer>> flowItem) {
        output.add(flowItem.value() * 10);
      }

      @Override
      public List<Integer> finalise() {
        return output;
      }
    };

    asyncFlow.enableImmediateFlushing();
    asyncFlow.addSubscriber(subscriber);
    asyncFlow.putItem(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    Integer[] intList = new Integer[] {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    asyncFlow.close(true);
    if (subscriber.getFutureResult().get().toArray().length > 10) {
      subscriber.getFutureResult().get().forEach(s -> logger.info(s.toString()));
    }
    assertArrayEquals(intList, subscriber.getFutureResult().get().toArray());

  }


  @Test
  public void shouldReturnHelloWorldFutureForSingleFutureSubscriber() throws Exception {
    FutureItemSubscriber<String, String> helloWorldSubscriber;
    FutureItemFlowable<String, String> asyncFlow = Flow.aFutureItemFlow(HELLO_WORLD_FLOW, 100,
        helloWorldSubscriber = new FutureItemSubscriber<>() {
          @Override
          public String onNext(Item<String, String> item) {
            logger.info("Completing future");
            return item.value() + " world";
          }

          @Override
          public void onError(Throwable error, Item<String, String> itemValue) {
            error.printStackTrace();
          }
        });
    Promise<String> promise = asyncFlow.submitItem("Hello");
    try {
      assertThat(promise.result(helloWorldSubscriber.getId()).get(1,
          TimeUnit.SECONDS), is("Hello world"));
    } catch (Exception e) {
      fail();
    } finally {
      asyncFlow.close(true);
    }

  }

  @Test
  public void shouldPingAndPong() throws ExecutionException, InterruptedException {
    ItemFlowable<String, Long> ping_stream = anItemFlow("ping_stream", 30);
    ItemFlowable<String, Long> pong_stream = anItemFlow("pong_stream", 30);
    ping_stream.enableImmediateFlushing();
    pong_stream.enableImmediateFlushing();
    ItemSubscriber<Long, String> pingSubscriber = new ItemSubscriber<Long, String>() {

      private int count = 0;

      @Override
      public void onNext(Item<String, Long> itemValue) {
        if (this.count < 4) {
          logger.info("got ping");
          pong_stream.putItemWithTTL(10, "pong");
          logger.info("sent pong");
          this.count++;
        }
        if (count == 4)
          exitAndReturn(System.currentTimeMillis());
      }

    };

    ItemSubscriber<Long, String> pongSubscriber = new ItemSubscriber<>() {

      private int count = 0;

      @Override
      public void onNext(Item<String, Long> itemValue) {
        if (this.count < 4) {
          logger.info("got pong");
          ping_stream.putItemWithTTL(10, "ping");
          logger.info("sent ping");
          this.count++;
        }
        if (count == 4)
          exitAndReturn(System.currentTimeMillis());
      }

    };
    ping_stream.addSubscriber(pingSubscriber);
    pong_stream.addSubscriber(pongSubscriber);
    ping_stream.putItem("ping");
    Thread.sleep(1000);
    logger.info(
        "Latency Ms = " + (pongSubscriber.getFutureResult().get() - pingSubscriber.getFutureResult()
            .get()));
    ping_stream.close(true);
    pong_stream.close(true);
  }

  @Test
  public void shouldSustainThroughputPerformanceTest() throws Exception {
    logger.info("Thread priority " + Thread.currentThread().getPriority());
    long start = System.currentTimeMillis();
    ItemFlowable<String, Long> asyncFlow = anItemFlow("sustainedPerformanceTest", 1);
    final ItemSubscriber<Long, String> subscriber = new ItemSubscriber<Long, String>() {
      long count;

      @Override
      public void initialise() {
        this.count = 0;
      }

      @Override
      public void onNext(Item<String, Long> itemValue) {
        this.count++;
      }

      @Override
      public Long finalise() {
        logger.info("Put " + this.count + " Items");
        return this.count;
      }
    };
    long loops = 10000000;
    asyncFlow.addSubscriber(subscriber);
    for (int i = 0; i < loops; i++) {
      asyncFlow.putItemWithTTL(1, "Hello world" + i);
    }
    asyncFlow.close(true);
    assertThat(subscriber.getFutureResult().get(), is(loops));
    logger.info("Items per second : " + loops / ((System.currentTimeMillis() - start) / 1000));
  }


  @Test
  public void shouldSustainThroughputPerformanceTestForMultipleSubscribers() throws Exception {
    long start = System.currentTimeMillis();
    ItemFlowable<String, Long> asyncFlow = anItemFlow("multisubcriberperformance", 0);

    List<Subscriber<Long, String>> subscriberInterfaceMap = new ArrayList<>();

    for (int i = 0; i < 100; i++) {

      final ItemSubscriber<Long, String> subscriber = new ItemSubscriber<Long, String>() {
        long count;

        @Override
        public void initialise() {
          this.count = 0;
        }

        @Override
        public void onNext(Item<String, Long> itemValue) {
          this.count++;
        }

        @Override
        public Long finalise() {
          return this.count;
        }
      };
      subscriberInterfaceMap.add(subscriber);
      asyncFlow.addSubscriber(subscriber);
    }


    long loops = 1000000;

    for (int i = 0; i < loops; i++) {
      asyncFlow.putItem("Hello world" + i);
    }


    Thread.sleep(1000);
    asyncFlow.close(true);

    subscriberInterfaceMap.stream().map(s -> {
      try {
        return s.getFutureResult().get();
      } catch (InterruptedException | ExecutionException e) {
        return 0;
      }
    }).forEach(s -> assertThat(s, is(loops)));

    logger.info("Items per second : " + loops / ((System.currentTimeMillis() - start) / 1000));
  }

  @Test
  public void stability() throws Exception {
    for (int i = 0; i < 5; i++) {
      shouldHandleExceptionForFutureSubscriber();
      shouldReturnHelloWorldItemFromFlowContents();
      shouldReturnHelloWorldFutureForSingleFutureSubscriber();
      shouldReturnInSequenceForFlowSubscriber();
      shouldSustainThroughputPerformanceTestForMultipleSubscribers();
    }
  }

  @Test
  public void shouldNotReturnDeadItemsAfter11Seconds() throws Exception {
    ItemFlowable<String, String> asyncFlow = anItemFlow("test_ttl", 60);
    asyncFlow.enableImmediateFlushing();
    asyncFlow.putItemWithTTL(1, "test1");
    asyncFlow.putItemWithTTL(10, "test10");
    asyncFlow.putItemWithTTL(20, "test20");
    asyncFlow.putItemWithTTL(60, "test60");


    Thread.sleep(11100);
    assertThat(asyncFlow.size(), is(2));
    assertThat(asyncFlow.contents().last().value(), is("test60"));
    asyncFlow.close(true);
  }

  @Ignore
  public void shouldReturnItemUsingIndexField() {
    ItemFlowable<TestObject, TestObject> asyncFlow = anItemFlow("test_index", "testField");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    asyncFlow.putItem(testObject1, testObject2, testObject3);
    asyncFlow.close(true);
    Assert.assertThat(asyncFlow.contents().item("hello1").value(), is(testObject1));
  }

  @Test
  public void shouldReturnItemUsingItemId() throws InterruptedException {
    ItemFlowable<TestObject, TestObject> asyncFlow;
    asyncFlow = Flow.<TestObject, TestObject>anItemFlow("test_item_id");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    long[] itemIds = asyncFlow.putItem(testObject1, testObject2, testObject3);
    Thread.sleep(100);
    Assert.assertThat(asyncFlow.contents().item(itemIds[1]).value(), is(testObject2));
    asyncFlow.close(true);
  }

  @Test
  public void shouldHandleExceptionForFutureSubscriber()
      throws ExecutionException, InterruptedException {
    FutureItemFlowable<String, String> asyncFlow =
        Flow.aFutureItemFlow(HELLO_WORLD_FLOW, new FutureItemSubscriber<String, String>() {

          @Override
          public String onNext(Item<String, String> itemValue) {
            return functionalThrow(itemValue.value());
          }

          private String functionalThrow(String itemValue) {
            if (itemValue.equals("Hello3"))
              throw new RuntimeException("hello");
            else
              return "happy";
          }

          @Override
          public void onError(Throwable error, Item<String, String> itemValue) {
            assert (error.getMessage().equals("hello"));
          }
        });
    Promise<String> promise1 = asyncFlow.submitItem("Hello1");
    Promise<String> promise2 = asyncFlow.submitItem("Hello2");
    Promise<String> promise3 = asyncFlow.submitItem("Hello3");

    try {
      assertThat(promise1.results().get(0).get(1, TimeUnit.SECONDS), is("happy"));
      assertThat(promise2.results().get(0).get(1, TimeUnit.SECONDS), is("happy"));
      assertThat(promise3.results().get(0).get(1, TimeUnit.SECONDS), is(nullValue()));
    } catch (TimeoutException e) {
      fail();
    } finally {
      asyncFlow.close(true);
    }

  }

  @Test
  public void shouldHandleErrorForFlowSubscriber() throws Exception {

    ArrayList<String> errors = new ArrayList<>();
    ItemFlowable<Integer, List<Integer>> asyncFlow = anItemFlow(INT_FLOW);
    asyncFlow.addSubscriber(new ItemSubscriber<>() {

      int count = 0;

      @Override
      public void onNext(Item<Integer, List<Integer>> itemValue) {
        count++;
        if (count == 2)
          throw new RuntimeException("hello");
      }

      @Override
      public void onError(Throwable error, Item<Integer, List<Integer>> itemValue) {
        errors.add(error.getMessage());
      }

      @Override
      public List<Integer> finalise() {
        return new ArrayList<>();
      }
    });
    asyncFlow.enableImmediateFlushing();
    asyncFlow.putItem(1, 2, 3, 4);
    asyncFlow.close(true);
    Assert.assertThat(errors.size(), is(1));
    Assert.assertThat(errors.get(0), is("hello"));
  }

  @Test
  public void testCallbackOnCompletion() throws InterruptedException {
    CompletableItemFlowable<String, String> asyncFlow =
        Flow.aCompletableItemFlow(HELLO_WORLD_FLOW, new CompletableItemSubscriber<>() {

          @Override
          public void onNext(Item<String, String> itemValue) throws Throwable {
            logger.info(Thread.currentThread() + ":OnNext" + itemValue.value());
            InternalExecutors.subscribersThreadPoolInstance().submit(new FutureTask<Void>(() -> {
              itemValue.completionHandler()
                  .completed(itemValue.value() + "World", itemValue.value());
              return null;
            }));
          }

          @Override
          public void onError(Throwable error, Item<String, String> itemValue) {

          }
        });

    FlowItemCompletionHandler<String, String> myHandler = new FlowItemCompletionHandler<>() {
      @Override
      public void completed(String result, String attachment) {
        logger.info(Thread.currentThread() + ":OnCallbackCompletion:" + result);

      }

      @Override
      public void failed(Throwable exc, String attachment) {
        exc.printStackTrace();
      }

    };

    logger.info(Thread.currentThread() + ":OnSubmit");
    for (int i = 0; i < 10; i++)
      asyncFlow.submitItem("Hello" + i, myHandler);
    asyncFlow.close(true);
  }


  public class TestObject {
    String testField;

    public TestObject(String testField) {
      this.testField = testField;
    }

    public String getTestField() {
      return testField;
    }
  }


}
