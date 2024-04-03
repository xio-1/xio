package org.xio.one.test.examples.logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.item.logging.AsyncCallbackItemLoggerService;
import org.xio.one.reactive.flow.domain.item.logging.LogLevel;
import org.xio.one.reactive.flow.domain.item.logging.SingleCallbackLoggerService;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class LoggerServiceTestShould {

  public static final String HELLO_LOG_ASYNC_ENTRY = "hello logAsync entry";
  private static int LOOP = 1000000;

  Logger logger = Logger.getLogger(LoggerServiceTestShould.class.getCanonicalName());

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
    logger
        .info("items per milli-second " + LOOP / ((System.currentTimeMillis() + 1 - start)));

    logger.info(loggerService.getLogFilePath().toString());
    loggerService.close();
  }

  @Test
  public void logs1MillionEntriesToFileAndMultiplexWithCallback() throws InterruptedException {

    long start = System.currentTimeMillis();
    AsyncCallbackItemLoggerService loggerService =
        AsyncCallbackItemLoggerService.logger(this.getClass());

    AtomicLong count = new AtomicLong();

    FlowItemCompletionHandler<Integer, String> itemCompletionHandler =
        new FlowItemCompletionHandler<Integer, String>() {

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
    logger
        .info("items per milli-second " + LOOP / ((System.currentTimeMillis() + 1 - start)));

    logger.info(loggerService.getLogFilePath().toString());
    loggerService.close(true);
  }



}
