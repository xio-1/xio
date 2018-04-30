package examples.logger.domain;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.domain.FutureResultFlowable;
import org.xio.one.reactive.flow.subscriber.FutureMultiplexItemSubscriber;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.WRITE;

public class AsyncFutureMultiplexLoggerService {

  private FutureResultFlowable<String, Integer> logEntryFlow;
  private Path logFilePath;
  private FutureMultiplexItemSubscriber<Integer, String> futureMultiplexItemSubscriber;

  public AsyncFutureMultiplexLoggerService(String canonicalName) throws IOException {
    logFilePath = File.createTempFile(canonicalName + "-", ".log").toPath();
    ByteBuffer buffer = ByteBuffer.allocate(1024 * 120000);
    logEntryFlow = Flow.aFutureResultFlowable(UUID.randomUUID().toString(),
        new FutureMultiplexItemSubscriber<>() {

          final AsynchronousFileChannel fileChannel =
              AsynchronousFileChannel.open(logFilePath, WRITE);
          long position = 0;

          @Override
          public void initialise() {
          }

          @Override
          public Map<Long, Future<Integer>> onNext(Stream<FlowItem<String>> entries) {

            Map<Long, Future<Integer>> futureMap = new ConcurrentHashMap<>();
            List<Long> itemIds = new ArrayList<>();


            CompletionHandler<Integer, Object> completionHandler =
                new CompletionHandler<Integer, Object>() {

                  @Override
                  public void completed(Integer result, Object attachment) {

                  }

                  @Override
                  public void failed(Throwable exc, Object attachment) {

                  }
                };

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
          public void onFutureCompletionError(Throwable error, String itemValue) {
            System.out.println("ERROR " + itemValue);
            error.printStackTrace();
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

  public static AsyncFutureMultiplexLoggerService logger(Class clazz) {
    try {
      return new AsyncFutureMultiplexLoggerService(clazz.getCanonicalName());
    } catch (IOException e) {
    }
    return null;
  }

  public Future<Integer> logAsync(LogLevel logLevel, String entry) {

    return logEntryFlow.submitItem(logLevel + ":" + entry);
  }

  public Path getLogFilePath() {
    return logFilePath;
  }

  public void close() {
    this.
        logEntryFlow.end(true);
  }
}
