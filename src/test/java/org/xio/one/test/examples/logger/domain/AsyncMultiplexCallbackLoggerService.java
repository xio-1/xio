package org.xio.one.test.examples.logger.domain;

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
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.WRITE;

public class AsyncMultiplexCallbackLoggerService {

  private CompletableItemFlowable<String, Integer> logEntryFlow;
  private Path logFilePath;
  private CompletableMultiItemSubscriber<Integer, String> futureMultiplexItemSubscriber;

  public AsyncMultiplexCallbackLoggerService(String canonicalName) throws IOException {
    logFilePath = File.createTempFile(canonicalName + "-", ".log").toPath();
    ByteBuffer buffer = ByteBuffer.allocate(1024 * 120000);
    logEntryFlow = Flow.aCompletableItemFlow(UUID.randomUUID().toString(),
        new CompletableMultiItemSubscriber<>(20) {

          final AsynchronousFileChannel fileChannel = AsynchronousFileChannel
              .open(logFilePath, Set.of(WRITE), InternalExecutors.ioThreadPoolInstance());
          long position = 0;

          @Override
          public void initialise() {
          }

          @Override
          public void onNext(Stream<CompletableItem<String, Integer>> entries) {

            List<FlowItemCompletionHandler<Integer, String>> callbacks = new ArrayList<>();

            entries.forEach(entry -> {
              buffer.put((entry.value() + "\r\n").getBytes());
              callbacks.add(entry.completionHandler());
            });
            buffer.flip();
            ByteBuffer toWrite = ByteBuffer.wrap(Arrays.copyOf(buffer.array(), buffer.limit()));

            CompletionHandler<Integer, Object> completionHandler = new CompletionHandler<>() {

              @Override
              public void completed(Integer result, Object attachment) {
                callbacks.stream().forEach(c -> c.completed(result, null));
              }

              @Override
              public void failed(Throwable exc, Object attachment) {
                callbacks.stream().forEach(c -> c.failed(exc, null));
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

  public static AsyncMultiplexCallbackLoggerService logger(Class clazz) {
    try {
      return new AsyncMultiplexCallbackLoggerService(clazz.getCanonicalName());
    } catch (IOException e) {
    }
    return null;
  }

  public void logAsync(LogLevel logLevel, String entry,
      FlowItemCompletionHandler<Integer, String> flowItemCompletionHandler) {
    logEntryFlow.submitItem(logLevel + ":" + entry, flowItemCompletionHandler);
  }

  public Path getLogFilePath() {
    return logFilePath;
  }

  public void close() {
    this.
        logEntryFlow.close(true);
  }
}
