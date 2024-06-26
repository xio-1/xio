package org.xio.one.test.examples.logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.logging.AsyncCallbackItemLoggerService;
import org.xio.one.reactive.flow.domain.item.logging.ItemLogger;
import org.xio.one.reactive.flow.domain.item.logging.LogLevel;
import org.xio.one.reactive.flow.domain.item.logging.SingleCallbackLoggerService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.xio.one.reactive.flow.Flow.anItemFlow;

public class ItemLoggerServiceTestShould {

  public static final String HELLO_LOG_ASYNC_ENTRY = "hello logAsync entry";
  private static int LOOP = 1000000;

  Logger logger = Logger.getLogger(ItemLoggerServiceTestShould.class.getCanonicalName());

  @BeforeClass
  public static void setup() {
    XIOService.start();
  }

  @AfterClass
  public static void tearDown() {
    XIOService.stop();
  }

  @Test
  public void logs1MillionEntriesToFileAndSingleWithCallback() throws InterruptedException {
    testSingleWithCallback(false);
  }

  @Test
  public void logs1MillionEntriesInParallelToFileAndSingleWithCallback()
      throws InterruptedException {
    testSingleWithCallback(true);
  }

  private void testSingleWithCallback(boolean parallel) throws InterruptedException {
    long start = System.currentTimeMillis();
    ArrayList<Future<Integer>> results = new ArrayList<>();
    SingleCallbackLoggerService loggerService =
        SingleCallbackLoggerService.logger(this.getClass(), parallel);

    AtomicLong count = new AtomicLong();

    FlowItemCompletionHandler<Integer, String> itemCompletionHandler =
        new FlowItemCompletionHandler<>() {

          @Override
          public void completed(Integer result, String attachment) {
            count.incrementAndGet();
          }

          @Override
          public void failed(Throwable exc, String attachment) {

          }

        };

    for (int i = 0; i < LOOP; i++)
      loggerService.logAsync(LogLevel.INFO, "hello logAsync entry->" + i, itemCompletionHandler);
    logger.info("logged in " + (System.currentTimeMillis() - start) / 1000);

    while (count.get() < LOOP) {
      Thread.sleep(100);
    }

    logger.info("to disk in " + (System.currentTimeMillis() - start) / 1000);
    logger.info("items per milli-second " + LOOP / ((System.currentTimeMillis() + 1 - start)));

    logger.info(loggerService.getLogFilePath().toString());
    loggerService.close();
  }

  @Test
  public void logs1MillionItemsToFileAndMultiplexWithCallback() throws InterruptedException {

    long start = System.currentTimeMillis();
    AsyncCallbackItemLoggerService<String> itemLoggerService =
        AsyncCallbackItemLoggerService.logger(this.getClass());

    AtomicLong count = new AtomicLong();

    FlowItemCompletionHandler<Void, Item<String>> itemCompletionHandler =
        new FlowItemCompletionHandler<Void, Item<String>>() {

          @Override
          public void completed(Void result, Item<String> attachment) {
            count.incrementAndGet();
          }

          @Override
          public void failed(Throwable exc, Item<String> attachment) {

          }

        };

    for (int i = 0; i < LOOP; i++) {
      Item<String> loggedItem = new Item("test" + i, i);
      itemLoggerService.logItem(loggedItem, itemCompletionHandler);
    }
    logger.info("logged in " + (System.currentTimeMillis() - start) / 1000);

    while (count.get() < LOOP) {
      Thread.sleep(100);
    }

    logger.info("to disk in " + (System.currentTimeMillis() - start) / 1000);
    logger.info("items per milli-second " + LOOP / ((System.currentTimeMillis() + 1 - start)));

    logger.info(itemLoggerService.getLogFilePath().toString());
    itemLoggerService.close(true);
  }

  @Test
  public void logs1MillionItemsToFileAndMultiplexWithoutCallback() throws InterruptedException {

    long start = System.currentTimeMillis();
    AsyncCallbackItemLoggerService<String> itemLoggerService =
        AsyncCallbackItemLoggerService.logger(this.getClass());

    for (int i = 0; i < LOOP; i++) {
      Item<String> loggedItem = new Item<>("test" + i, i);
      itemLoggerService.logItem(loggedItem);
    }
    logger.info("logged in " + (System.currentTimeMillis() - start) / 1000);

    itemLoggerService.close(true);

    logger.info("to disk in " + (System.currentTimeMillis() - start) / 1000);
    logger.info("items per milli-second " + LOOP / ((System.currentTimeMillis() + 1 - start)));

    logger.info(itemLoggerService.getLogFilePath().toString());
    itemLoggerService.close(true);
    assertEquals(itemLoggerService.getNumberOfItemsWritten(), (long) LOOP);
  }

}
