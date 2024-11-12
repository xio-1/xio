package org.xio.one.reactive.flow.domain.item.logging;

import static java.nio.file.StandardOpenOption.WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.CompletableItemFlowable;
import org.xio.one.reactive.flow.domain.flow.FlowItemCompletionHandler;
import org.xio.one.reactive.flow.domain.item.CompletableItem;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.CompletableMultiItemSubscriber;
import org.xio.one.reactive.flow.util.InternalExecutors;

public class AsyncCallbackItemLoggerService<T> implements ItemLogger<T> {

  private static final Logger logger =
      Logger.getLogger(AsyncCallbackItemLoggerService.class.getCanonicalName());
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH':'mm'Z'"); // Quoted "Z" to
  private final File logFile;
  private final CompletableItemFlowable<Item<T>, Void> logEntryFlow;
  AtomicLong numberOfEntries = new AtomicLong(0);
  AtomicLong numberOfFileWrites = new AtomicLong(0);
  private Path logFilePath;
  private final Object lock = new Object();

  public AsyncCallbackItemLoggerService(String fileName, ItemSerializer<T> itemSerializer,
      int bufferSize, byte[] delim)
      {
        try {
          this.logFile = createItemLogFile(fileName);

        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
    logEntryFlow = Flow.aCompletableItemFlow(UUID.randomUUID().toString(),
        new CompletableMultiItemSubscriber<Void, Item<T>>(20) {
          final AsynchronousFileChannel fileChannel = AsynchronousFileChannel
              .open(logFile.toPath(), Set.of(WRITE), InternalExecutors.ioThreadPoolInstance());

          long position = 0;

          @Override
          public void initialise() {

          }


          @Override
          public void onNext(Stream<CompletableItem<Item<T>, Void>> entries) {
            synchronized (lock) {
              AtomicLong newEntries = new AtomicLong();
              List<CompletableItem<Item<T>, Void>> entryList = entries.toList();
              CompletableItem[] items = entryList.toArray(new CompletableItem[entryList.size()]);
              if (items.length > 0) {
                for (CompletableItem each : items) {
                  List<FlowItemCompletionHandler<Void, Item<T>>> callbacks = new ArrayList<>();
                  buffer.put(itemSerializer.serialize(each, Optional.of(delim)));
                  callbacks.add(each.flowItemCompletionHandler());
                  newEntries.getAndIncrement();
                  numberOfEntries.getAndIncrement();
                  if (newEntries.get() % 1009 == 0) {
                    buffer.flip();
                    doFileWrite(callbacks, newEntries);
                  }
                }
                if (newEntries.get() % 1009 != 0) {
                  buffer.flip();
                  doFileWrite(new ArrayList<>(), newEntries);
                }
              }
            }
          }

          private void doFileWrite(List<FlowItemCompletionHandler<Void, Item<T>>> callbacks,
              AtomicLong newEntries) {
            ByteBuffer toWrite = ByteBuffer.wrap(Arrays.copyOf(buffer.array(), buffer.limit()));

            CompletionHandler<Integer, Item<T>> fileWriteCompletionHandler = new CompletionHandler<>() {

              @Override
              public void completed(Integer value, Item<T> attachment) {
                callbacks.forEach(c -> c.completed(null, attachment));
              }

              @Override
              public void failed(Throwable exc, Item<T> attachment) {
                callbacks.forEach(c -> c.failed(exc, attachment));
              }
            };

            try {
              fileChannel.write(toWrite, position, null, fileWriteCompletionHandler);
              position = position + buffer.limit();

            } catch (Exception e) {
              logger.severe(e.getMessage());
              callbacks.stream().forEach(c -> c.failed(e, null));
              throw new RuntimeException(e);
            } finally {
              buffer.clear();
            }
          }

          @Override
          public Void finalise() {
            synchronized (lock) {
              try {
                fileChannel.close();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return null;
            }
          }
        });
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

  }

  // indicate UTC, no timezone offset
  private static String getDate() {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DATE_FORMAT.setTimeZone(tz);
    return DATE_FORMAT.format(new Date());
  }

  public static <T> AsyncCallbackItemLoggerService logger(Class clazz,
      ItemSerializer<T> itemSerializer) {

      return new AsyncCallbackItemLoggerService<T>(clazz.getCanonicalName(), itemSerializer,
          1024 * 1024 * 4, "\n".getBytes());

  }

  public static <T> AsyncCallbackItemLoggerService logger(String filename,
      ItemSerializer<T> itemSerializer) {

      return new AsyncCallbackItemLoggerService<T>(filename, itemSerializer, 1024 * 1024 * 4,
          "\n".getBytes());

  }

  private static File createItemLogFile(String filename) throws IOException {
    String home = System.getProperty("user.home");
    new File(home + "/logs").mkdir();
    new File(home + "/logs/replay").mkdir();
    File logFile = new File(home + "/logs/replay", filename);
    logFile.delete();
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
    while (!logEntryFlow.hasEnded()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public long getNumberOfItemsWritten() {
    return numberOfEntries.get();
  }

}