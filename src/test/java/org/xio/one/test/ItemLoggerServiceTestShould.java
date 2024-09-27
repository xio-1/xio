package org.xio.one.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.domain.item.logging.AsyncCallbackItemLoggerService;
import org.xio.one.reactive.flow.domain.item.logging.LogLevel;
import org.xio.one.reactive.flow.domain.item.logging.SingleCallbackLoggerService;
import org.xio.one.test.utils.SimpleJSONSerializer;

public class ItemLoggerServiceTestShould {

  public static final String HELLO_LOG_ASYNC_ENTRY = "hello logAsync entry";
  private static final int LOOP = 1000000;

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
    testSingleWithCallback("sync-log.dat", false);
  }

  @Test
  public void logs1MillionEntriesInParallelToFileAndSingleWithCallback()
      throws InterruptedException {
    testSingleWithCallback("sync-parallel-log.dat", true);
  }

  private void testSingleWithCallback(String filename, boolean parallel) throws InterruptedException {
    long start = System.currentTimeMillis();
    ArrayList<Future<Integer>> results = new ArrayList<>();
    SingleCallbackLoggerService loggerService =
        SingleCallbackLoggerService.logger(filename, parallel);

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

    for (int i = 0; i < LOOP; i++) {
      loggerService.logAsync(LogLevel.INFO, "hello logAsync entry->" + i,
          itemCompletionHandler);
    }
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
        AsyncCallbackItemLoggerService.logger("asynclog-callback.dat", new SimpleJSONSerializer<>());

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
        AsyncCallbackItemLoggerService.logger("asynclog-nocallback.dat", new SimpleJSONSerializer<>());

    for (int i = 0; i < LOOP; i++) {
      Item<String> loggedItem = new Item<>("test" + i, i);
      itemLoggerService.logItem(loggedItem);
    }
    logger.info("logged in " + (System.currentTimeMillis() - start) / 1000);
    logger.info("to disk in " + (System.currentTimeMillis() - start) / 1000);
    logger.info("items per milli-second " + LOOP / ((System.currentTimeMillis() + 1 - start)));
    logger.info(itemLoggerService.getLogFilePath().toString());
    itemLoggerService.close(true);
    assertEquals(itemLoggerService.getNumberOfItemsWritten(), LOOP);
  }

}
