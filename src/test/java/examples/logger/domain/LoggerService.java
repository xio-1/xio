package examples.logger.domain;

import org.xio.one.reactive.flow.Flowable;
import org.xio.one.reactive.flow.domain.Item;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.core.MultiplexSubscriber;

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
  private Flowable<String, Boolean> itemLoop;
  private Path logFilePath;

  public LoggerService(String canonicalName) throws IOException {
    String fileName = UUID.randomUUID().toString() + canonicalName + ".tmp";
    logFilePath = File.createTempFile(fileName, null).toPath();
    AsynchronousFileChannel fileChannel =
        AsynchronousFileChannel.open(logFilePath, WRITE, CREATE, READ);

    itemLoop = Flow.flow();

    loggerSubscriber = new MultiplexSubscriber<Boolean, String>() {

      private long position = 0;

      @Override
      public Optional<Map<Long, Boolean>> onNext(Stream<Item<String>> items) throws Throwable {
        ByteBuffer buffer = ByteBuffer.allocate(64738);
        items.forEach(item -> {
          buffer.put((item.value() + "\r\n").getBytes());
        });
        buffer.flip();
        fileChannel.write(buffer, position);
        position = position + buffer.limit();
        buffer.clear();
        return Optional.empty();
      }

      @Override
      public Optional<Object> onError(Throwable error, Stream<Item<String>> itemValue) {
        return Optional.empty();
      }
    };

    itemLoop.addMultiplexSubscriber(loggerSubscriber);

  }

  public static LoggerService logger(Class clazz) {
    try {
      return new LoggerService(clazz.getCanonicalName());
    } catch (IOException e) {
    }
    return null;
  }

  public void logAsync(LogLevel logLevel, String entry) {
    itemLoop.putItem(logLevel + ":" + entry);
  }

  public Path getLogFilePath() {
    return logFilePath;
  }

  public void close() {
    itemLoop.end(true);
  }
}
