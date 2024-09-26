package org.xio.one.reactive.flow.domain.item.logging;

import static java.nio.file.StandardOpenOption.WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.util.Set;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.CompletableItemFlowable;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.flow.IOCompletionHandler;
import org.xio.one.reactive.flow.domain.item.CompletableItem;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.CompletableItemSubscriber;
import org.xio.one.reactive.flow.util.InternalExecutors;

public class SingleCallbackLoggerService {

  private final Path logFilePath;
  private final CompletableItemFlowable<String, Integer> logEntryFlow;

  public SingleCallbackLoggerService(String canonicalName, boolean parallel) throws IOException {
    logFilePath = File.createTempFile(canonicalName + "-", ".log").toPath();
    //ByteBuffer buffer = ByteBuffer.allocate(1024 * 120000);
    logEntryFlow =
        Flow.aCompletableItemFlow("logger", 10, new CompletableItemSubscriber<>(parallel) {

          final AsynchronousFileChannel fileChannel = AsynchronousFileChannel
              .open(logFilePath, Set.of(WRITE), InternalExecutors.ioThreadPoolInstance());
          long position = 0;

          @Override
          public void initialise() {
          }

          @Override
          public void onNext(CompletableItem<String, Integer> entry) {

            try {
              CompletionHandler<Integer, String> completionHandler =
                  IOCompletionHandler.aIOCompletionHandler(entry.flowItemCompletionHandler());
              ByteBuffer buffer = ByteBuffer.wrap((entry.getItemValue() + "\n").getBytes());
              //buffer = buffer.flip();
              fileChannel.write(buffer, position, null, completionHandler);
              position = position + buffer.limit();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          @Override
          public void onError(Throwable error, Item<String> itemValue) {
            throw new RuntimeException(error);
          }

          @Override
          public Integer finalise() {
            try {
              fileChannel.close();
              return 0;
            } catch (IOException e) {
              e.printStackTrace();
              return -1;
            }
          }
        });

  }

  public static SingleCallbackLoggerService logger(Class clazz, boolean parallel) {
    try {
      return new SingleCallbackLoggerService(clazz.getCanonicalName(), parallel);
    } catch (IOException e) {
    }
    return null;
  }

  public void logAsync(LogLevel logLevel, String entry,
      FlowItemCompletionHandler<Integer, String> completionHandler) {
    logEntryFlow.submitItem(logLevel + ":" + entry, completionHandler);
  }

  public Path getLogFilePath() {
    return logFilePath;
  }

  public void close() {
    this.
        logEntryFlow.close(true);
  }
}
