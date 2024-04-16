package org.xio.one.reactive.flow.domain.item.logging;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.CompletableItemFlowable;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.item.CompletableItem;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.CompletableMultiItemSubscriber;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.WRITE;

public class AsyncCallbackItemLoggerService<T> implements ItemLogger<T> {
  private static Logger logger =
      Logger.getLogger(AsyncCallbackItemLoggerService.class.getCanonicalName());
  private final File logFile;
  private CompletableItemFlowable<Item<T>, Void> logEntryFlow;
  private Path logFilePath;
  AtomicLong numberOfEntries = new AtomicLong(0);
  AtomicLong numberOfFileWrites = new AtomicLong(0);
  private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH':'mm'Z'"); // Quoted "Z" to
  // indicate UTC, no timezone offset
  private static String getDate() {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DATE_FORMAT.setTimeZone(tz);
    return DATE_FORMAT.format(new Date());
  }

  public AsyncCallbackItemLoggerService(String fileName) throws IOException {
    this.logFile = createItemLogFile(fileName);
    ByteBuffer buffer = ByteBuffer.allocate(1024 * 120000);
    logEntryFlow = Flow.aCompletableItemFlow(UUID.randomUUID().toString(),
        new CompletableMultiItemSubscriber<Void,Item<T>>(20) {
          final AsynchronousFileChannel fileChannel = AsynchronousFileChannel
              .open(logFile.toPath(), Set.of(WRITE), InternalExecutors.ioThreadPoolInstance());
          long position = 0;

          @Override
          public void initialise() {
          }

          @Override
          public void onNext(Stream<CompletableItem<Item<T>, Void>> entries) {
            List<FlowItemCompletionHandler<Void, Item<T>>> callbacks = new ArrayList<>();
            AtomicLong newEntries= new AtomicLong();
            entries.forEach(entry -> {
              buffer.put((entry.value().toString() + "\r\n").getBytes());
              callbacks.add(entry.flowItemCompletionHandler());
              newEntries.getAndIncrement();
            });
            buffer.flip();
            ByteBuffer toWrite = ByteBuffer.wrap(Arrays.copyOf(buffer.array(), buffer.limit()));

            CompletionHandler<Integer, Item<T>> fileWriteCompletionHandler = new CompletionHandler<>() {

              @Override
              public void completed(Integer value, Item<T> attachment) {
                callbacks.forEach(c -> c.completed(null, attachment));
              }

              @Override
              public void failed(Throwable exc, Item<T> attachment) {
                callbacks.stream().forEach(c -> c.failed(exc, attachment));
              }
            };

            try {
              fileChannel.write(toWrite, position, null, fileWriteCompletionHandler);
              position = position + buffer.limit();
              numberOfEntries.addAndGet(newEntries.get());
            } catch (Exception e) {
              logger.severe(e.getMessage());
              callbacks.stream().forEach(c -> c.failed(e, null));
              throw new RuntimeException(e);
            } finally {
              buffer.clear();
            }
            return;
          }


          @Override
          public Void finalise() {
            try {
              fileChannel.close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            return null;
          }
        });

  }

  public static <T> AsyncCallbackItemLoggerService logger(Class clazz) {
    try {
      return new AsyncCallbackItemLoggerService<T>(clazz.getCanonicalName());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> AsyncCallbackItemLoggerService logger(String filename) {
    try {
      return new AsyncCallbackItemLoggerService<T>(filename);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static File createItemLogFile(String filename) throws IOException {
    String home = System.getProperty("user.home");
    new File(home + "/logs").mkdir();
    new File(home + "/logs/replay").mkdir();
    File logFile = new File(home + "/logs/replay",  filename + "-" + getDate() + ".log");
    logFile.createNewFile();
    return logFile;
  }

  public void logItem(Item<T> entry,
      FlowItemCompletionHandler<Void, Item<T>> flowItemCompletionHandler) {
    logEntryFlow.submitItem(entry, flowItemCompletionHandler);
  }

  public void logItem(Item<T> entry) {
    logEntryFlow.submitItem(entry, new FlowItemCompletionHandler<Void, Item<T>>() {
      @Override
      public void completed(Void result, Item<T> attachment) {

      }

      @Override
      public void failed(Throwable exc, Item<T> attachment) {

      }
    });
  }

  public Path getLogFilePath() {
    return logFile.toPath();
  }

  public void close(boolean waitForEnd) {
    this.
        logEntryFlow.close(waitForEnd);
  }

  public long getNumberOfItemsWritten() {
    return numberOfEntries.get();
  }

}
