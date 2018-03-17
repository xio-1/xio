package examples.logger.domain;

import org.xio.one.stream.event.Event;
import org.xio.one.stream.reactive.AsyncStream;
import org.xio.one.stream.reactive.subscribers.MultiplexSubscriber;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.*;

public class LoggerService {

  private MultiplexSubscriber<Boolean, String> loggerSubscriber;
  private AsyncStream<String, Boolean> eventLoop;
  private Path logFilePath;

  public LoggerService(String canonicalName) throws IOException {
    String fileName = UUID.randomUUID().toString() + canonicalName + ".tmp";
    logFilePath = File.createTempFile(fileName, null).toPath();
    AsynchronousFileChannel fileChannel =
        AsynchronousFileChannel.open(logFilePath, WRITE, CREATE, READ);

    eventLoop = new AsyncStream<>(UUID.randomUUID().toString());

    loggerSubscriber = new MultiplexSubscriber<Boolean, String>() {

      private long position = 0;

      @Override
      public Optional<Map<Long, Boolean>> onNext(Stream<Event<String>> events) throws Throwable {
        ByteBuffer buffer = ByteBuffer.allocate(64738);
        events.forEach(event -> {
          buffer.put((event.value() + "\r\n").getBytes());
        });
        buffer.flip();
        fileChannel.write(buffer, position);
        position = position + buffer.limit();
        buffer.clear();
        return Optional.empty();
      }

      @Override
      public Optional<Object> onError(Throwable error, Stream<Event<String>> eventValue) {
        return Optional.empty();
      }
    };

    eventLoop.withMultiplexSubscriber(loggerSubscriber);

  }

  public static LoggerService logger(Class clazz) {
    try {
      return new LoggerService(clazz.getCanonicalName());
    } catch (IOException e) {
    }
    return null;
  }

  public void logAsync(LogLevel logLevel, String entry) {
    eventLoop.putValue(logLevel + ":" + entry);
  }

  public Path getLogFilePath() {
    return logFilePath;
  }

  public void close() {
    eventLoop.end(true);
  }
}
