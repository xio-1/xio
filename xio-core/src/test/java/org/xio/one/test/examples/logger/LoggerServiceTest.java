package org.xio.one.test.examples.logger;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.test.examples.logger.domain.AsyncFutureMultiplexLoggerService;
import org.xio.one.test.examples.logger.domain.AsyncMultiplexCallbackLoggerService;
import org.xio.one.test.examples.logger.domain.LogLevel;
import org.xio.one.test.examples.logger.domain.SingleCallbackLoggerService;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;

public class LoggerServiceTest {

  public static final String HELLO_LOG_ASYNC_ENTRY = "hello logAsync entry";
  private static int ONE_MILLION = 1000000;

  Logger logger = Logger.getLogger(LoggerServiceTest.class.getCanonicalName());

  @Test
  public void createEmptyLogFileOnCreation() {
    AsyncFutureMultiplexLoggerService loggerService =
        AsyncFutureMultiplexLoggerService.logger(this.getClass());
    loggerService.close();
    Assert.assertTrue(loggerService.getLogFilePath().toFile().exists());
  }

  @Test
  public void logsOneEntryToFileUsingMultiplexLoggerService() throws Exception {
    AsyncFutureMultiplexLoggerService loggerService =
        AsyncFutureMultiplexLoggerService.logger(this.getClass());
    Future<Integer> bytesLogged = loggerService.logAsync(LogLevel.INFO, HELLO_LOG_ASYNC_ENTRY);
    loggerService.close();
    Assert.assertThat(bytesLogged.get(),
        is(("INFO" + ":" + HELLO_LOG_ASYNC_ENTRY + "\r\n").getBytes().length));
    logger.info(loggerService.getLogFilePath().toString());
  }

  @Test
  public void logs1MillionFutureEntriesToFile() {

    long start = System.currentTimeMillis();
    ArrayList<Future<Integer>> results = new ArrayList<>();
    AsyncFutureMultiplexLoggerService loggerService =
        AsyncFutureMultiplexLoggerService.logger(this.getClass());
    for (int i = 0; i < ONE_MILLION; i++)
      results.add(loggerService.logAsync(LogLevel.INFO, "hello logAsync entry->" + i));
    logger.info("logged in " + (System.currentTimeMillis() - start) / 1000);

    results.forEach(i -> {
      try {
        i.get();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    });
    logger.info("to disk in " + (System.currentTimeMillis() - start) / 1000);

    logger
        .info("items per milli-second " + ONE_MILLION / ((System.currentTimeMillis() + 1 - start)));

    logger.info(loggerService.getLogFilePath().toString());
    loggerService.close();
  }

  @Test
  public void muiltithreadlogs1MillionFutureEntriesToFile() {

    long start = System.currentTimeMillis();
    ArrayList<Future<Integer>> results = new ArrayList<>();
    AsyncFutureMultiplexLoggerService loggerService =
        AsyncFutureMultiplexLoggerService.logger(this.getClass());

    for (int z = 0; z < 50; z++)
      new Thread(() -> {
        for (int i = 0; i < ONE_MILLION / 50; i++)
          results.add(loggerService.logAsync(LogLevel.INFO, "hello logAsync entry->" + i));
        logger.info("logged in " + (System.currentTimeMillis() - start) / 1000);
      }).run();

    results.stream().parallel().forEach(i -> {
      try {
        i.get(5000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      } catch (TimeoutException e) {
        e.printStackTrace();
      }
    });
    logger.info("to disk in " + (System.currentTimeMillis() - start) / 1000);

    logger.info("items per milli-second " + ONE_MILLION / ((System.currentTimeMillis() - start)));

    logger.info(loggerService.getLogFilePath().toString());
    loggerService.close();
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
    ArrayList<Future<Integer>> results = new ArrayList<>();
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


  @Test
  public void stability() {
    muiltithreadlogs1MillionFutureEntriesToFile();
    muiltithreadlogs1MillionFutureEntriesToFile();
    muiltithreadlogs1MillionFutureEntriesToFile();
    muiltithreadlogs1MillionFutureEntriesToFile();
    logs1MillionFutureEntriesToFile();
    logs1MillionFutureEntriesToFile();
    logs1MillionFutureEntriesToFile();
    logs1MillionFutureEntriesToFile();
  }


}