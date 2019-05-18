package org.xio.one.test.examples.logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.test.examples.logger.domain.AsyncMultiplexCallbackLoggerService;
import org.xio.one.test.examples.logger.domain.LogLevel;
import org.xio.one.test.examples.logger.domain.SingleCallbackLoggerService;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class LoggerServiceTest {

  public static final String HELLO_LOG_ASYNC_ENTRY = "hello logAsync entry";
  private static int ONE_MILLION = 1000000;

  Logger logger = Logger.getLogger(LoggerServiceTest.class.getCanonicalName());

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

    for (int i = 0; i < ONE_MILLION; i++)
      loggerService.logAsync(LogLevel.INFO, "hello logAsync entry->" + i, itemCompletionHandler);
    logger.info("logged in " + (System.currentTimeMillis() - start) / 1000);

    while (count.get() < ONE_MILLION) {
      Thread.sleep(100);
    }

    logger.info("to disk in " + (System.currentTimeMillis() - start) / 1000);
    logger
        .info("items per milli-second " + ONE_MILLION / ((System.currentTimeMillis() + 1 - start)));

    logger.info(loggerService.getLogFilePath().toString());
    loggerService.close();
  }

  @Test
  public void logs1MillionEntriesToFileAndMultiplexWithCallback() throws InterruptedException {

    long start = System.currentTimeMillis();
    AsyncMultiplexCallbackLoggerService loggerService =
        AsyncMultiplexCallbackLoggerService.logger(this.getClass());

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

    for (int i = 0; i < ONE_MILLION; i++)
      loggerService.logAsync(LogLevel.INFO, "hello logAsync entry->" + i, itemCompletionHandler);
    logger.info("logged in " + (System.currentTimeMillis() - start) / 1000);

    while (count.get() < ONE_MILLION) {
      Thread.sleep(100);
    }

    logger.info("to disk in " + (System.currentTimeMillis() - start) / 1000);
    logger
        .info("items per milli-second " + ONE_MILLION / ((System.currentTimeMillis() + 1 - start)));

    logger.info(loggerService.getLogFilePath().toString());
    loggerService.close();
  }



}
