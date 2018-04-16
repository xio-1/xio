package examples.logger.domain;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.Flowable;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.subscriber.FutureMultiplexItemSubscriber;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.WRITE;

public class AsyncFutureMultiplexLoggerService {

  private Flowable<String, Integer> logEntryFlow;
  private Path logFilePath;
  private FutureMultiplexItemSubscriber<Integer, String> futureMultiplexItemSubscriber;

  public AsyncFutureMultiplexLoggerService(String canonicalName) throws IOException {
    logFilePath = File.createTempFile(canonicalName + "-", ".log").toPath();
    logEntryFlow = Flow.aFlowable(UUID.randomUUID().toString(), 0);
    ByteBuffer buffer = ByteBuffer.allocate(1024 * 120000);

    futureMultiplexItemSubscriber = new FutureMultiplexItemSubscriber<Integer, String>() {

      final AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(logFilePath, WRITE);
      long position = 0;

      @Override
      public void initialise() {
      }

      @Override
      public Map<Long, Future<Integer>> onNext(Stream<FlowItem<String>> entries) {
        Map<Long, Future<Integer>> futureMap = new ConcurrentHashMap<>();
        List<Long> itemIds = new ArrayList<>();

        entries.forEach(entry -> {
          buffer.put((entry.value() + "\r\n").getBytes());
          itemIds.add(entry.itemId());
        });
        buffer.flip();
        ByteBuffer toWrite = ByteBuffer.wrap(Arrays.copyOf(buffer.array(), buffer.limit()));

        try {
          //fileChannel.lock(position, toWrite.limit(), true);
          Future<Integer> bytesWrittenFuture = fileChannel.write(toWrite, position);
          if (bytesWrittenFuture != null) {
            position = position + buffer.limit();
            itemIds.stream().forEach(i -> {
              futureMap.put(i, bytesWrittenFuture);
            });
          } else
            System.currentTimeMillis();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          buffer.clear();
        }
        return futureMap;
      }

      @Override
      public void onFutureError(Throwable error, String itemValue) {
        System.out.println("ERROR " + itemValue);
        error.printStackTrace();
      }

      @Override
      protected void finalize() throws Throwable {
        fileChannel.close();
      }
    };

  }

  public static AsyncFutureMultiplexLoggerService logger(Class clazz) {
    try {
      return new AsyncFutureMultiplexLoggerService(clazz.getCanonicalName());
    } catch (IOException e) {
    }
    return null;
  }

  public Future<Integer> logAsync(LogLevel logLevel, String entry) {
    return logEntryFlow.putItem(logLevel + ":" + entry, futureMultiplexItemSubscriber);
  }

  public Path getLogFilePath() {
    return logFilePath;
  }

  public void close() {
    this.
        logEntryFlow.end(true);
  }
}
