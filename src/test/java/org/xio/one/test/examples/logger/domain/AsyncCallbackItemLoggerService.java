package org.xio.one.test.examples.logger.domain;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.CompletableItemFlowable;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.item.CompletableItem;
import org.xio.one.reactive.flow.subscribers.CompletableMultiItemSubscriber;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.WRITE;

public class AsyncCallbackItemLoggerService<T,R> {

  private static Logger logger =
      Logger.getLogger(AsyncCallbackItemLoggerService.class.getCanonicalName());
  private final File logFile;
  private CompletableItemFlowable<T, R> logEntryFlow;
  private Path logFilePath;

  private static String getDate() {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH':'mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
    df.setTimeZone(tz);
    return df.format(new Date());
  }

  public AsyncCallbackItemLoggerService(String fileName) throws IOException {
    this.logFile = createLogFile(fileName);
    ByteBuffer buffer = ByteBuffer.allocate(1024 * 120000);
    logEntryFlow = Flow.aCompletableItemFlow(UUID.randomUUID().toString(),
        new CompletableMultiItemSubscriber<R,T>(20) {

          final AsynchronousFileChannel fileChannel = AsynchronousFileChannel
              .open(logFile.toPath(), Set.of(WRITE), InternalExecutors.ioThreadPoolInstance());
          long position = 0;

          @Override
          public void initialise() {
          }

          @Override
          public void onNext(Stream<CompletableItem<T, R>> entries) {

            List<FlowItemCompletionHandler<R, T>> callbacks = new ArrayList<>();

            entries.forEach(entry -> {
              buffer.put((entry.value() + "\r\n").getBytes());
              callbacks.add(entry.completionHandler());
            });
            buffer.flip();
            ByteBuffer toWrite = ByteBuffer.wrap(Arrays.copyOf(buffer.array(), buffer.limit()));

            CompletionHandler<Integer, T> completionHandler = new CompletionHandler<>() {


              @Override
              public void completed(Integer result, T attachment) {
                callbacks.forEach(c -> c.completed(null, attachment));
              }

              @Override
              public void failed(Throwable exc, T attachment) {
                callbacks.stream().forEach(c -> c.failed(exc, attachment));
              }
            };

            try {
              fileChannel.write(toWrite, position, null, completionHandler);
              position = position + buffer.limit();
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              buffer.clear();
            }
            return;
          }


          @Override
          public R finalise() {
            try {
              fileChannel.close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            return null;
          }
        });

  }

  public static AsyncCallbackItemLoggerService logger(Class clazz) {
    try {
      return new AsyncCallbackItemLoggerService(clazz.getCanonicalName());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static File createLogFile(String filename) throws IOException {
    String home = System.getProperty("user.home");
    new File(home + "/logs").mkdir();
    File logFile = new File(home + "/logs/",  filename + "-" + getDate() + ".log");
    logFile.createNewFile();
    return logFile;
  }

  public void logAsync(LogLevel logLevel, T entry,
      FlowItemCompletionHandler<R, T> flowItemCompletionHandler) {
    logEntryFlow.submitItem(entry, flowItemCompletionHandler);
  }

  public Path getLogFilePath() {
    return logFile.toPath();
  }

  public void close(boolean waitForEnd) {
    this.
        logEntryFlow.close(waitForEnd);
  }
}
