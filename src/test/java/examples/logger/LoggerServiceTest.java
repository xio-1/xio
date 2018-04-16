package examples.logger;

import examples.logger.domain.AsyncFutureMultiplexLoggerService;
import examples.logger.domain.LogLevel;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;

public class LoggerServiceTest {

  public static final String HELLO_LOG_ASYNC_ENTRY = "hello logAsync entry";
  private static int ONE_MILLION = 1000000;

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
    System.out.println(loggerService.getLogFilePath().toString());
  }

  @Test
  public void logs1MillionFutureEntriesToFile() {

    long start = System.currentTimeMillis();
    ArrayList<Future<Integer>> results = new ArrayList<>();
    AsyncFutureMultiplexLoggerService loggerService =
        AsyncFutureMultiplexLoggerService.logger(this.getClass());
    for (int i = 0; i < ONE_MILLION; i++)
      results.add(loggerService.logAsync(LogLevel.INFO, "hello logAsync entry->" + i));
    System.out.println("logged in " + (System.currentTimeMillis() - start) / 1000);

    results.forEach(i -> {
      try {
        i.get();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    });
    System.out.println("to disk in " + (System.currentTimeMillis() - start) / 1000);

    System.out
        .println("items per second " + ONE_MILLION / ((System.currentTimeMillis() - start) / 1000));

    System.out.println(loggerService.getLogFilePath().toString());
    loggerService.close();
  }

  @Test
  public void muiltithreadlogs1MillionFutureEntriesToFile() {

    long start = System.currentTimeMillis();
    ArrayList<Future<Integer>> results = new ArrayList<>();
    AsyncFutureMultiplexLoggerService loggerService =
        AsyncFutureMultiplexLoggerService.logger(this.getClass());

    for (int z = 0; z < 4; z++)
      new Thread(() -> {
        for (int i = 0; i < ONE_MILLION / 4; i++)
          results.add(loggerService.logAsync(LogLevel.INFO, "hello logAsync entry->" + i));
        System.out.println("logged in " + (System.currentTimeMillis() - start) / 1000);
      }).run();

    results.stream().forEach(i -> {
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
    System.out.println("to disk in " + (System.currentTimeMillis() - start) / 1000);

    System.out
        .println("items per second " + ONE_MILLION / ((System.currentTimeMillis() - start) / 1000));

    System.out.println(loggerService.getLogFilePath().toString());
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
