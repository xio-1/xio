package examples.logger;

import examples.logger.domain.LogLevel;
import examples.logger.domain.LoggerService;
import org.junit.Assert;
import org.junit.Test;

public class LoggerServiceTest {

  @Test
  public void createEmptyLogFileOnCreation() {
    LoggerService loggerService = LoggerService.logger(this.getClass());
    loggerService.close();
    Assert.assertTrue(loggerService.getLogFilePath().toFile().exists());
  }

  @Test
  public void logsOneEntryToFile() {
    LoggerService loggerService = LoggerService.logger(this.getClass());
    loggerService.logAsync(LogLevel.INFO, "hello logAsync entry");
    loggerService.close();
    System.out.println(loggerService.getLogFilePath().toString());
  }

  @Test
  public void logs100000EntriesToFile() {
    LoggerService loggerService = LoggerService.logger(this.getClass());
    for (int i = 0; i < 100000; i++)
      loggerService.logAsync(LogLevel.INFO, "hello logAsync entry->"+i);
    loggerService.close();
    System.out.println(loggerService.getLogFilePath().toString());
  }

}
