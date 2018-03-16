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
    loggerService.log(LogLevel.INFO, "hello log entry");
    loggerService.close();
    System.out.println(loggerService.getLogFilePath().toString());
  }

  @Test
  public void logs100000EntriesToFile() {
    LoggerService loggerService = LoggerService.logger(this.getClass());
    for (int i = 0; i < 100000; i++)
      loggerService.log(LogLevel.INFO, "hello log entry->"+i);
    loggerService.close();
    System.out.println(loggerService.getLogFilePath().toString());
  }

}
