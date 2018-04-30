package examples.logger.domain;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.*;
import org.xio.one.reactive.flow.subscriber.CompletableItemSubscriber;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.WRITE;

public class SingleCallbackLoggerService {

  private Path logFilePath;
  private CompletableItemFlowable<String, Integer> logEntryFlow;

  public SingleCallbackLoggerService(String canonicalName) throws IOException {
    logFilePath = File.createTempFile(canonicalName + "-", ".log").toPath();
    ByteBuffer buffer = ByteBuffer.allocate(1024 * 120000);
    logEntryFlow =
        Flow.aCompletableResultFlowable(new CompletableItemSubscriber<>() {

          final AsynchronousFileChannel fileChannel =
              AsynchronousFileChannel.open(logFilePath, WRITE);
          long position = 0;

          @Override
          public void initialise() {
          }

          @Override
          public void onNext(CompletableFlowItem<Integer, String> entry) {

            try {
              CompletionHandler<Integer, String> completionHandler =
                  IOCompletionHandler.aIOCompletionHandler(entry.completionHandler());
              fileChannel.write(ByteBuffer.wrap(entry.value().getBytes()), position, null,
                  completionHandler);
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              buffer.clear();
            }
          }

          @Override
          public void onError(Throwable error, FlowItem<String> itemValue) {

          }

          @Override
          public void finalise() {
            try {
              fileChannel.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        });

  }

  public static SingleCallbackLoggerService logger(Class clazz) {
    try {
      return new SingleCallbackLoggerService(clazz.getCanonicalName());
    } catch (IOException e) {
    }
    return null;
  }

  public void logAsync(LogLevel logLevel, String entry,
      ItemCompletionHandler<Integer, String> completionHandler) {
    logEntryFlow.submitItem(logLevel + ":" + entry, completionHandler);
  }

  public Path getLogFilePath() {
    return logFilePath;
  }

  public void close() {
    this.
        logEntryFlow.end(true);
  }
}
