package test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.CompletableItemFlowable;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.flow.FutureItemResultFlowable;
import org.xio.one.reactive.flow.domain.flow.ItemFlow;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.CompletableItemSubscriber;
import org.xio.one.reactive.flow.subscribers.FutureItemSubscriber;
import org.xio.one.reactive.flow.subscribers.FutureMultiplexItemSubscriber;
import org.xio.one.reactive.flow.subscribers.StreamItemSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.SubscriberInterface;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class FlowTest {

    public static final int NUMBER_OF_ITEMS = 1000000;
    final Logger logger = Logger.getLogger(FlowTest.class.getCanonicalName());
    String HELLO_WORLD_FLOW = "helloWorldFlow";
    String INT_FLOW = "integerFlow";

    @Test
    public void shouldReturnHelloWorldItemFromFlowContents() throws InterruptedException {
        ItemFlow<String, String> asyncFlow = Flow.anItemFlow(HELLO_WORLD_FLOW);
        asyncFlow.enableImmediateFlushing();
        asyncFlow.putItem("Hello world");
        Thread.sleep(100);
        assertThat(asyncFlow.contents().last().value().toString(), is("Hello world"));
        asyncFlow.close(true);

    }

    @Test
    public void simpleExample() throws Exception {

        ItemFlow<String, String> toUPPERcaseFlow = Flow.anItemFlow();

        toUPPERcaseFlow.addSubscriber(new StreamItemSubscriber<>() {

            @Override
            public void onNext(Item<String, String> itemValue) {
                logger.info(itemValue.value().toUpperCase());
            }

        });

        toUPPERcaseFlow.putItem("value1", "value2");
        toUPPERcaseFlow.close(true);
    }

    @Test
    public void simpleItemStreamExampleWithFunctionalStyle() throws Exception {
        ItemFlow<String, String> toUPPERcaseFlow = Flow.anItemFlow();
        toUPPERcaseFlow.putItem("Value1", "Value2");
        toUPPERcaseFlow.publish().doOnNext(i -> logger.info(i.value().toUpperCase())).subscribe();
        toUPPERcaseFlow.publish().doOnNext(i -> logger.info(i.value().toLowerCase())).subscribe();
        toUPPERcaseFlow.publish().onStart(()-> logger.fine("I starting")).doOnNext(i -> {
            logger.info("I happy " + i.value());
            throw new RuntimeException("I ask help " + i.value());
        }).doOnError((e, i) -> logger.warning(e.getMessage())).onEnd(()->logger.fine("I finish")).subscribe();
        toUPPERcaseFlow.close(true);
    }


    @Test
    public void shouldReturnInSequenceForFlowSubscriber() throws Exception {

        ItemFlow<Integer, List<Integer>> asyncFlow = Flow.anItemFlow(INT_FLOW);

        StreamItemSubscriber<List<Integer>, Integer> subscriber = new StreamItemSubscriber<>() {

            private List<Integer> output = new ArrayList<>();

            @Override
            public void onNext(Item<Integer, List<Integer>> flowItem) {
                output.add(flowItem.value() * 10);
            }

            @Override
            public void finalise() {
                this.setResult(output);
            }
        };

        asyncFlow.enableImmediateFlushing();
        asyncFlow.addSubscriber(subscriber);
        asyncFlow.putItem(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Integer[] intList = new Integer[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        asyncFlow.close(true);
        if (subscriber.getResult().get().toArray().length > 10) {
            subscriber.getResult().get().forEach(s -> logger.info(s.toString()));
        }
        assertArrayEquals(intList, subscriber.getResult().get().toArray());

    }


    @Test
    public void shouldReturnHelloWorldFutureForSingleFutureSubscriber() throws Exception {
        FutureItemResultFlowable<String, String> asyncFlow =
                Flow.aFutureResultItemFlow(HELLO_WORLD_FLOW, 100, new FutureItemSubscriber<String, String>() {
                    @Override
                    public Future<String> onNext(String itemValue) {
                        logger.info("Completing future");
                        return CompletableFuture.completedFuture(itemValue + " world");
                    }

                    @Override
                    public void onFutureCompletionError(Throwable error, String itemValue) {
                        error.printStackTrace();
                    }
                });
        Future<String> result = asyncFlow.submitItem("Hello");
        try {
            assertThat(result.get(1, TimeUnit.SECONDS), is("Hello world"));
        } catch (Exception e) {
            fail();
        }
        finally {
            asyncFlow.close(true);
        }

    }

    @Test
    public void shouldPingAndPong() throws ExecutionException, InterruptedException {
        ItemFlow<String, Long> ping_stream = Flow.anItemFlow("ping_stream");
        ItemFlow<String, Long> pong_stream = Flow.anItemFlow("pong_stream");
        ping_stream.enableImmediateFlushing();
        pong_stream.enableImmediateFlushing();
        StreamItemSubscriber<Long, String> pingSubscriber = new StreamItemSubscriber<Long, String>() {

            private int count = 0;

            @Override
            public void onNext(Item<String, Long> itemValue) {
                if (this.count < 4) {
                    logger.info("got ping");
                    pong_stream.putItemWithTTL(100, "pong");
                    logger.info("sent pong");
                    this.count++;
                }
                if (count == 4)
                    setResult(System.currentTimeMillis());
            }

        };

        StreamItemSubscriber<Long, String> pongSubscriber = new StreamItemSubscriber<>() {

            private int count = 0;

            @Override
            public void onNext(Item<String, Long> itemValue) {
                if (this.count < 4) {
                    logger.info("got pong");
                    ping_stream.putItemWithTTL(100, "ping");
                    logger.info("sent ping");
                    this.count++;
                }
                if (count == 4)
                    setResult(System.currentTimeMillis());
            }

        };
        ping_stream.addSubscriber(pingSubscriber);
        pong_stream.addSubscriber(pongSubscriber);
        ping_stream.putItem("ping");
        Thread.sleep(1000);
        logger.info("Latency Ms = " + (pongSubscriber.getResult().get() - pingSubscriber.getResult().get()));
        ping_stream.close(true);
        pong_stream.close(true);
    }

    @Test
    public void shouldSustainThroughputPerformanceTest() throws Exception {
        long start = System.currentTimeMillis();
        ItemFlow<String, Long> asyncFlow = Flow.anItemFlow("sustainedPerformanceTest", 1);
        final StreamItemSubscriber<Long, String> subscriber = new StreamItemSubscriber<Long, String>() {
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
            public void finalise() {
                this.setResult(this.count);
            }
        };
        long loops = 1000000;
        asyncFlow.addSubscriber(subscriber);
        for (int i = 0; i < loops; i++) {
            asyncFlow.putItemWithTTL(1, "Hello world" + i);
        }
        asyncFlow.close(true);
        assertThat(subscriber.getResult().get(), is(loops));
        logger.info("Items per second : " + 10000000 / ((System.currentTimeMillis() - start) / 1000));
    }


    @Test
    public void shouldSustainThroughputPerformanceTestForMultipleSubscribers() throws Exception {
        long start = System.currentTimeMillis();
        ItemFlow<String, Long> asyncFlow = Flow.anItemFlow("multisubcriberperformance", 0);

        List<SubscriberInterface<Long, String>> subscriberInterfaceMap = new ArrayList<>();

        for (int i = 0; i < 100; i++) {

            final StreamItemSubscriber<Long, String> subscriber = new StreamItemSubscriber<Long, String>() {
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
                public void finalise() {
                    this.setResult(this.count);
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
                return s.getResult().get();
            } catch (InterruptedException | ExecutionException e) {
                return 0;
            }
        }).forEach(s -> assertThat(s, is(loops)));

        logger.info("Items per second : " + 10000000 / ((System.currentTimeMillis() - start) / 1000));
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
    public void putForMultiplexingFutures() throws Exception {
        FutureItemResultFlowable<String, String> micro_stream =
                Flow.aFutureResultItemFlow("micro_stream", new FutureMultiplexItemSubscriber<>() {

                    @Override
                    public Map<Long, Future<String>> onNext(Stream<Item<String, String>> e) {
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
        try {
            assertThat(result1.get(1,TimeUnit.SECONDS), is("HELLO1"));
            assertThat(result2.get(1, TimeUnit.SECONDS), is("HELLO2"));
            assertThat(result3.get(1,TimeUnit.SECONDS), is("HELLO3"));
        } catch (Exception e) {
            fail();
        } finally {
            micro_stream.close(true);
        }
    }

    @Test
    public void shouldRemoveExpiredItemsAfter1Second() throws Exception {
        ItemFlow<String, String> asyncFlow = Flow.anItemFlow("test_ttl");
        asyncFlow.enableImmediateFlushing();
        asyncFlow.putItemWithTTL(10, "test10");
        asyncFlow.putItemWithTTL(1, "test1");
        asyncFlow.putItemWithTTL(1, "test2");
        asyncFlow.putItemWithTTL(1, "test3");
        Thread.sleep(1100);
        assertThat(asyncFlow.size(), is(1));
        assertThat(asyncFlow.contents().last().value(), is("test10"));
        asyncFlow.close(true);
    }

    @Ignore
    public void shouldReturnItemUsingIndexField() {
        ItemFlow<TestObject, TestObject> asyncFlow = Flow.anItemFlow("test_index", "testField");
        TestObject testObject1 = new TestObject("hello1");
        TestObject testObject2 = new TestObject("hello2");
        TestObject testObject3 = new TestObject("hello3");
        asyncFlow.putItem(testObject1, testObject2, testObject3);
        asyncFlow.close(true);
        Assert.assertThat(asyncFlow.contents().item("hello1").value(), is(testObject1));
    }

    @Test
    public void shouldReturnItemUsingItemId() throws InterruptedException {
        ItemFlow<TestObject, TestObject> asyncFlow;
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
        FutureItemResultFlowable<String, String> asyncFlow =
                Flow.aFutureResultItemFlow(HELLO_WORLD_FLOW, new FutureItemSubscriber<String, String>() {

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

        try {
            assertThat(result1.get(1, TimeUnit.SECONDS), is("happy"));
            assertThat(result2.get(1,TimeUnit.SECONDS), is("happy"));
            assertThat(result3.get(1,TimeUnit.SECONDS), is(nullValue()));
        } catch (TimeoutException e) {
            fail();
        } finally {
            asyncFlow.close(true);
        }

    }

    @Test
    public void shouldHandleErrorForFlowSubscriber() throws Exception {

        ArrayList<String> errors = new ArrayList<>();
        ItemFlow<Integer, List<Integer>> asyncFlow = Flow.anItemFlow(INT_FLOW);
        asyncFlow.addSubscriber(new StreamItemSubscriber<>() {

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
            public void finalise() {
                this.setResult(new ArrayList<Integer>());
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
