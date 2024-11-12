package org.xio.one.reactive.flow;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.xio.one.reactive.flow.Flow.anItemFlow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xio.one.reactive.flow.domain.flow.CompletableItemFlowable;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.flow.FutureItemFlowable;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.item.CompletableItem;
import org.xio.one.reactive.flow.domain.item.EmptyItem;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.logging.AsyncCallbackItemLoggerService;
import org.xio.one.reactive.flow.internal.RecoverySnapshot;
import org.xio.one.reactive.flow.subscribers.CompletableItemSubscriber;
import org.xio.one.reactive.flow.subscribers.FutureItemSubscriber;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;
import org.xio.one.reactive.flow.util.InternalExecutors;

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
    asyncFlow.publishTo(ItemSubscriber.class).doForEach(i -> logger.info(i.getItemValue())).subscribe();
    asyncFlow.putItem("Hello World!!!");
    asyncFlow.close(true);
  }

  @Test
  public void shouldReturnHelloWorldItemFromFlowContents() throws InterruptedException {
    ItemFlowable<String, Void> asyncFlow = anItemFlow(HELLO_WORLD_FLOW);
    asyncFlow.enableImmediateFlushing();
    asyncFlow.putItem("Hello world");
    Item<String>[] snapshot = asyncFlow.takeSinkSnapshot();
    assertThat(snapshot[snapshot.length - 1].getItemValue(), is("Hello world"));
    asyncFlow.close(true);

  }

  @Test
  public void shouldReturnRecoverySnapshot() throws InterruptedException, IOException {
    RecoverySnapshot<String, String> snapshot = generateSnapshot(true);
    assertThat(snapshot.getContents().first().getItemValue(), is("Hello world"));
    assertThat(snapshot.getContents().last().getItemValue(), is("World hello"));
    assertThat(snapshot.getItemID(), is(2L));
    assertThat(snapshot.getLastSeenItemMap().values().iterator().next().getItemValue(), is("World hello"));
  }

  //ToDo add context document
  public RecoverySnapshot<String, String> generateSnapshot(boolean full)
      throws InterruptedException, IOException {
    ItemFlowable<String, String> asyncFlow = anItemFlow(HELLO_WORLD_FLOW);
    asyncFlow.enableImmediateFlushing();
    asyncFlow.addItemLogger(
        new AsyncCallbackItemLoggerService<String>("./log_snapshot_hello.dat",
            new ObjectToByteArrayJSONSerializer<>(), 1024*240000, "\n".getBytes())
    );
    asyncFlow.putItem("Hello world");
    asyncFlow.putItem("World hello");
    ItemSubscriber<String, String> subscriber = new ItemSubscriber<String, String>() {

      private String lastValue;

      @Override
      public void onNext(Item<String> item) {
        lastValue = item.getItemValue();
      }

      @Override
      public String finalise() {
        return this.lastValue;
      }

      @Override
      public Map<String, Object> getContext() {
        return Map.of("lastValue", lastValue);
      }
    };
    asyncFlow.addSubscriber(subscriber);
    Thread.sleep(1000);
    RecoverySnapshot<String, String> snapshot = asyncFlow.takeRecoverySnapshot(full);
    asyncFlow.putItem("after 1");
    asyncFlow.putItem("after 2");
    asyncFlow.close(true);
    return snapshot;
  }

  @Test
  public void itemSerialise() {
    Item<String> item = new Item<>("hello", 10001,1001);
    ObjectToByteArrayJSONSerializer<String> s = new ObjectToByteArrayJSONSerializer<>();
    logger.log(Level.INFO, new String(s.serialize(item, Optional.empty())));
  }

  @Test
  public void itemDeserialise() {
    String json = "{\"itemValue\":\"hello\",\"itemId\":10001,\"itemTimestamp\":1727843073596,\"itemNodeId\":7758320006798585198,\"itemTTLSeconds\":1001,\"alive\":true}";
    ObjectToByteArrayJSONSerializer<String> s = new ObjectToByteArrayJSONSerializer<>();
    logger.log(Level.INFO, s.deserialize(json.getBytes(),Optional.empty()).getItemValue());
  }

  @Test
  //it should be noted that a streams contents (items) are final but a subscriber can contain any type of user code and
  //therefore requires custom context for recovery.
  public void shouldRecoverSnapshotToFlow()
      throws InterruptedException, ExecutionException, IOException {
    RecoverySnapshot<String, String> snapshot = generateSnapshot(true);
    ItemFlowable<String, String> recoveredFlow = anItemFlow("RECOVERY");
    recoveredFlow.recoverSnapshot(snapshot);
    assertThat(snapshot.getContents().first().getItemValue(), is("Hello world"));
    assertThat(snapshot.getContents().last().getItemValue(), is("World hello"));
    assertThat(snapshot.getLastSeenItemMap().values().iterator().next().getItemValue(), is("World hello"));
    assertThat(snapshot.getSubscriberContext()
            .get(snapshot.getLastSeenItemMap().keySet().iterator().next()).get("lastValue"),
        is("World hello"));
    RestorableSubscriberImpl restoreSubscriberImpl = new RestorableSubscriberImpl();
    recoveredFlow.restoreAllSubscribers(snapshot.getSubscriberContext(),restoreSubscriberImpl);
    recoveredFlow.putItem("Goodbye world");
    Future<String> value =
        recoveredFlow.getSubscriber(snapshot.getSubscriberContext().keySet().iterator().next()).getFutureResult();
    recoveredFlow.close(true);
    assertThat(value.get(), is("Goodbye world"));
  }

  @Test
  public void shouldRecoverFlowFromLog()
      throws IOException, InterruptedException, ExecutionException {
    RecoverySnapshot<String, String> snapshot = generateSnapshot(true);
    ItemFlowable<String, String> recoveredFlow = anItemFlow("RECOVERYNLOG",
        AsyncCallbackItemLoggerService.logger("asynclog-nocallback-recovered.dat", new ObjectToByteArrayJSONSerializer<>()));
    recoveredFlow.recoverSnapshot(snapshot);
    assertThat(snapshot.getContents().first().getItemValue(), is("Hello world"));
    assertThat(snapshot.getContents().last().getItemValue(), is("World hello"));
    assertThat(snapshot.getLastSeenItemMap().values().iterator().next().getItemValue(), is("World hello"));
    assertThat(snapshot.getSubscriberContext()
            .get(snapshot.getLastSeenItemMap().keySet().iterator().next()).get("lastValue"),
        is("World hello"));
    RestorableSubscriberImpl restoreSubscriberImpl = new RestorableSubscriberImpl();
    recoveredFlow.restoreAllSubscribers(snapshot.getSubscriberContext(),restoreSubscriberImpl);
    recoveredFlow.putItem("Goodbye world");
    Future<String> value =
        recoveredFlow.getSubscriber(snapshot.getSubscriberContext().keySet().iterator().next()).getFutureResult();
    recoveredFlow.close(true);
    assertThat(value.get(), is("Goodbye world"));

  }

  @Test
  public void flowItemLoggerTest() throws IOException {
    ItemFlowable<String, Void> asyncFlow = anItemFlow("LOG_HELLO");
    asyncFlow.enableImmediateFlushing();
    AsyncCallbackItemLoggerService<String> asyncFlowLogger =
        new AsyncCallbackItemLoggerService<String>("./log_hello.dat", new ObjectToByteArrayJSONSerializer<>(), 1024*1024*4,"\n".getBytes());
    asyncFlow.addItemLogger(asyncFlowLogger);
    for (int i = 0; i < 1000; i++) {
      asyncFlow.putItem("Hello world }}}" + i);
    }
    asyncFlow.close(true);
    asyncFlowLogger.close(true);
    assertEquals(asyncFlowLogger.getNumberOfItemsWritten(), 1000);
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
          public void onNext(Item<String> item) {
            logger.info("on next " + item.getItemValue());
            stringBuffer.append(item.getItemValue().toUpperCase());
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

    assertThat(upperCaseSubscriber.getFutureResult().get(),
        is("VALUE1 VALUE2 VALUE3 VALUE4 VALUE5"));
  }


  @Test
  public void shouldCountWords() {
    countWords(Arrays.stream("The quick quick brown fox".split(" "))).forEach(
        (key, value) -> logger.info(key + "," + value));
  }


  @Test
  public void shouldReturnInSequenceForFlowSubscriber() throws Exception {

    ItemFlowable<Integer, List<Integer>> asyncFlow = anItemFlow(INT_FLOW);

    ItemSubscriber<List<Integer>, Integer> subscriber = new ItemSubscriber<>() {

      private final List<Integer> output = new ArrayList<>();

      @Override
      public void onNext(Item<Integer> item) {
        output.add(item.getItemValue() * 10);
      }

      @Override
      public List<Integer> finalise() {
        return output;
      }
    };

    asyncFlow.enableImmediateFlushing();
    asyncFlow.addSubscriber(subscriber);
    asyncFlow.putItem(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    Integer[] intList = new Integer[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    asyncFlow.close(true);
    if (subscriber.getFutureResult().get().toArray().length > 10) {
      subscriber.getFutureResult().get().forEach(s -> logger.info(s.toString()));
    }
    assertArrayEquals(intList, subscriber.getFutureResult().get().toArray());

  }


  @Test
  public void shouldLogItemsToFile() throws InterruptedException {

  }


  @Test
  public void shouldReturnHelloWorldFutureForSingleFutureSubscriber() throws Exception {
    FutureItemSubscriber<String, String> helloWorldSubscriber;
    FutureItemFlowable<String, String> asyncFlow = Flow.aFutureItemFlow(HELLO_WORLD_FLOW, 100,
        helloWorldSubscriber = new FutureItemSubscriber<>() {
          @Override
          public String onNext(Item<String> item) {
            logger.info("Completing future");
            return item.getItemValue() + " world";
          }

          @Override
          public void onError(Throwable error, Item<String> itemValue) {
            error.printStackTrace();
          }
        });
    Promise<String> promise = asyncFlow.submitItem("Hello");
    try {
      assertThat(promise.result(helloWorldSubscriber.getId()).get(1, TimeUnit.SECONDS),
          is("Hello world"));
    } catch (Exception e) {
      e.printStackTrace();
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
      public void onNext(Item<String> item) {
        if (this.count < 4) {
          logger.info("got ping");
          pong_stream.putItemWithTTL(10, "pong");
          logger.info("sent pong");
          this.count++;
        }
        if (count == 4) {
          exitAndReturn(System.currentTimeMillis());
        }
      }

    };

    ItemSubscriber<Long, String> pongSubscriber = new ItemSubscriber<>() {

      private int count = 0;

      @Override
      public void onNext(Item<String> item) {
        if (this.count < 4) {
          logger.info("got pong");
          ping_stream.putItemWithTTL(10, "ping");
          logger.info("sent ping");
          this.count++;
        }
        if (count == 4) {
          exitAndReturn(System.currentTimeMillis());
        }
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
      public void onNext(Item<String> item) {
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
        public void onNext(Item<String> item) {
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

    long loops = 2000000;

    for (int i = 0; i < loops; i++) {
      asyncFlow.putItem("Hello world" + i);
    }

    asyncFlow.close(true);

    subscriberInterfaceMap.stream().map(s -> {
      try {
        return s.getFutureResult().get();
      } catch (InterruptedException | ExecutionException e) {
        return 0;
      }
    }).forEach(s -> assertThat(s, is(loops)));

    logger.info("Items per second : " + loops / ((System.currentTimeMillis()+1 - start) / 1000));
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
    assertThat(asyncFlow.getSink().lastItem().getItemValue(), is("test60"));
    asyncFlow.close(true);
  }

  @Test
  public void shouldReturnItemUsingItemId() throws InterruptedException {
    ItemFlowable<TestObject, Void> asyncFlow;
    asyncFlow = Flow.anItemFlow("test_item_id");
    TestObject testObject1 = new TestObject("hello1");
    TestObject testObject2 = new TestObject("hello2");
    TestObject testObject3 = new TestObject("hello3");
    long[] itemIds = asyncFlow.putItem(testObject1, testObject2, testObject3);
    assertThat(asyncFlow.takeSinkSnapshot()[1].getItemValue(), is(testObject2));
    asyncFlow.close(true);
  }

  @Test
  public void shouldHandleExceptionForFutureSubscriber()
      throws ExecutionException, InterruptedException {
    FutureItemFlowable<String, String> asyncFlow =
        Flow.aFutureItemFlow(HELLO_WORLD_FLOW, new FutureItemSubscriber<String, String>() {

          @Override
          public String onNext(Item<String> itemValue) {
            return functionalThrow(itemValue.getItemValue());
          }

          private String functionalThrow(String itemValue) {
            if (itemValue.equals("Hello3")) {
              throw new RuntimeException("hello");
            } else {
              return "happy";
            }
          }

          @Override
          public void onError(Throwable error, Item<String> itemValue) {
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
      public void onNext(Item<Integer> item) {
        count++;
        if (count == 2) {
          throw new RuntimeException("hello");
        }
      }

      @Override
      public void onError(Throwable error, Item<Integer> item) {
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
    assertThat(errors.size(), is(1));
    assertThat(errors.get(0), is("hello"));
  }

  @Test
  public void testCallbackOnCompletion() throws InterruptedException {
    CompletableItemFlowable<String, String> asyncFlow =
        Flow.aCompletableItemFlow(HELLO_WORLD_FLOW, new CompletableItemSubscriber<>() {

          @Override
          public void onNext(CompletableItem<String, String> itemValue) throws Throwable {
            logger.info(Thread.currentThread() + ":OnNext" + itemValue.getItemValue());
            InternalExecutors.subscribersTaskThreadPoolInstance()
                .submit(new FutureTask<Void>(() -> {
                  itemValue.flowItemCompletionHandler()
                      .completed(itemValue.getItemValue() + "World", itemValue.getItemValue());
                  return null;
                }));
          }

          @Override
          public void onError(Throwable error, Item<String> itemValue) {

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
    for (int i = 0; i < 10; i++) {
      asyncFlow.submitItem("Hello" + i, myHandler);
    }
    asyncFlow.close(true);
  }

  @Test
  public void shouldReprocessAfterResettingTheLastSeenItem() throws InterruptedException {
    ItemFlowable<Integer, List<Item<Integer>>> asyncFlow = anItemFlow(INT_FLOW, 100);
    ArrayList<Item<Integer>> itemStore = new ArrayList<>();
    AtomicInteger count = new AtomicInteger(0);
    Subscriber<List<Item<Integer>>, Integer> subscriber =
        asyncFlow.addSubscriber(new ItemSubscriber<>() {

          @Override
          public void onNext(Item<Integer> item) {
            itemStore.add(item);
            count.incrementAndGet();
          }

          @Override
          public void onError(Throwable error, Item<Integer> item) {

          }

          @Override
          public List<Item<Integer>> finalise() {
            return itemStore;
          }
        });
    asyncFlow.enableImmediateFlushing();
    asyncFlow.putItem(1, 2, 3, 4);
    Thread.sleep(1000);
    asyncFlow.resetLastSeenItem(subscriber.getId(), EmptyItem.EMPTY_ITEM);
    asyncFlow.close(true);
    assertThat(count.get(), is(8));
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
