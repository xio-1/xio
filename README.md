# XIO Guide

The xio-core library is a strictly ordered reactive flow and event sink that supports futures and 
micro 
batching.  

You can use XIO for your async requirements such as event loops, realtime flow processing, 
batching API calls and async I/O and futures.   

XIO is small, fast, lightweight and easy to use with with zero dependencies on other frameworks!

To build xio-core go to the directory xio-core ```mvn clean package```

This s/w is still in beta phase and so minor changes are expected to some method signatures.

Once ready we will be publishing to maven central, until then please reference the unit tests for
 the latest syntax and examples thanks!
 
The xio-http module is in alpha and can be used for distributed eventing with websockets.

## ItemFlowable<T,R>  

An async flow where you will put values to be processed by your subscriber. 

ItemFlowable respects the sequence in which your values were put as items to the flow and will 
provide them to a subscriber in the order they were received.  

T is the type of the value you will put to the flow.  R is the type of the value to be returned by a subscriber. 

For example the below creates a flow that takes String items and returns a String as a result. 

You can see that the putItem method can be used to add items to the Flow. 

```
ItemFlowable<String, String> toUPPERCASEFlow = anItemFlow();

toUPPERCASEFlow.putItem("value1", "value2", "value3", "value4", "value5");
    
```

In order to process items in a flow you will need to write subscriber code to receive the Item<T> 
values put into the Flow. You can get the actual value of T by calling the value() method on the 
given Item<T> i.e ```itemValue.value();```

## Subscriber<R,T>  

A subscriber to a Flowable must implement the Subscriber<R,T> interface, a subscriber recieves each Item<T> in sequence to process.  T is the type of the value you will put to the flow.  

R is the type of the value to be returned by this subscriber when the flow is ended. 

There are currently 4 subscriber types provided out of the box. Two basic async subscriber types (fire and forget) and two "transactional" async CompleteableFuture based subscriber types (fire, and in the future acknowledge (success, failure) via a callback).

Basic subscribers are expected to manage their own successful and compensating actions with no 
acknowledgement (callback) back to the submitter. 

Where this is needed the CompleteableItemSubscriber subscriber allows you to link each submitted 
item to a future async result via a callback.

Completeable Future subscribers are useful where the caller (item submitter) must know (in the 
future) if an item was processed successfully (or not), a good example of this would be a 
transaction log for example where it is crucial that every transaction is logged or on failure rolled back.

### ItemSubscriber<R,T> 

A subscriber that subscribes to each Item<T> in the flow in turn and on finalisation can return a value that has been computed (across all items if required).

For example the below code will log every value put to the flow and transform each value to Upper Case and add it to a StringBuffer. When the flow is ended it returns the contents of all the computed upper case values as a String from the StringBuffer.

```
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
            logger.info("on next " + item.value());
            stringBuffer.append(item.value().toUpperCase());
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
```

Or if you prefer functional style programming then;
```
    ItemFlowable<String, String> toUPPERCASEFlow = anItemFlow();

    StringBuffer buff = new StringBuffer();

    Subscriber<String, String> upperCaseSubscriber =
        toUPPERCASEFlow
            .publishTo(ItemSubscriber.class)
            .doForEach(i -> buff.append(i.value().toUpperCase()).append(" "))
            .andOnEndReturn(() -> buff.toString().trim())
            .subscribe();

    toUPPERCASEFlow.putItem("value1", "value2", "value3", "value4", "value5");

    toUPPERCASEFlow.close(true);

    Assert.assertThat(upperCaseSubscriber.getFutureResult().get(),
        is("VALUE1 VALUE2 VALUE3 VALUE4 VALUE5"));
```

### FutureItemSubscriber<R,T>

Where a future result for every item submitted is required then use the FutureItemSubscriber in 
conjunction with the submitItem method to return a future Promise for each item submitted to the 
flow.

```
    FutureItemSubscriber<String, String> helloWorldSubscriber;
    FutureItemFlowable<String, String> asyncFlow = Flow.aFutureItemFlow(HELLO_WORLD_FLOW, 100,
        helloWorldSubscriber = new FutureItemSubscriber<>() {
          @Override
          public String onNext(Item<String> item) {
            logger.info("Completing future");
            return item.value() + " world";
          }

          @Override
          public void onError(Throwable error, Item<String> itemValue) {
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

``` 

Or if you prefer functional style programming then;

    FutureItemFlowable<String, String> toUPPERCASEFlow = Flow.aFutureItemFlow();

    StringBuffer buff = new StringBuffer();

    Subscriber<String, String> upperCaseSubscriber =
        toUPPERCASEFlow
            .publishTo(FutureItemSubscriber.class)
            .onStart(() -> logger.info("I am starting"))
            .returnForEach(i -> i.value() + " world")
            .finallyOnEnd(() -> logger.info("I am done"))
            .subscribe();

    Assert
        .assertThat(toUPPERCASEFlow.submitItem("Hello").result(upperCaseSubscriber.getId()).get(),
            is("Hello world"));
    
    toUPPERCASEFlow.close(true);
    
### CompletableItemSubscriber<R,T>

XIO also supports async callbacks i.e. where after an async task you want some code to be called 
back e.g. to return a result to another process

```
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
    for (int i = 0; i < 10; i++)
      asyncFlow.submitItem("Hello" + i, myHandler);
    asyncFlow.close(true);
```

## Example projects

There are to examples given in the test package

Bank - demonstrates how XIO can be used to implement a simple async event loop, see the BankService 
class. 

Logger - shows how async IO can be batched using XIO, see public class 
AsyncMultiplexCallbackLoggerService.
