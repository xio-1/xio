package examples.logger.domain;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.Flowable;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.subscriber.MultiplexItemSubscriber;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.*;

public class LoggerService {

  private Flowable<String, Boolean> logEntryFlow;
  private Path logFilePath;

  public LoggerService(String canonicalName) throws IOException {
    String fileName = UUID.randomUUID().toString() + canonicalName + ".tmp";
    logFilePath = File.createTempFile(fileName, null).toPath();

    AsynchronousFileChannel fileChannel =
        AsynchronousFileChannel.open(logFilePath, WRITE, CREATE, READ);

    logEntryFlow = Flow.aFlowable();

    logEntryFlow.addMultiplexSubscriber(new MultiplexItemSubscriber<Boolean, String>() {
      private long position = 0;
      @Override
      public void onNext(Stream<FlowItem<String>> entries) {
        ByteBuffer buffer = ByteBuffer.allocate(64738);
        entries.forEach(entry -> {
          buffer.put((entry.value() + "\r\n").getBytes());
        });
        buffer.flip();
        fileChannel.write(buffer, position);
        position = position + buffer.limit();
        buffer.clear();
      }
    });

  }

  public static LoggerService logger(Class clazz) {
    try {
      return new LoggerService(clazz.getCanonicalName());
    } catch (IOException e) {
    }
    return null;
  }

  public void logAsync(LogLevel logLevel, String entry) {
    logEntryFlow.putItem(logLevel + ":" + entry);
  }

  public Path getLogFilePath() {
    return logFilePath;
  }

  public void close() {
    logEntryFlow.end(true);
  }
}
