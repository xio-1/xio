package org.xio.one.reactive.flow;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerializedByteLogFileReader {

  Logger logger = Logger.getLogger(FunctionalStyleTest.class.getName());

  public void readFile(String filename) {
    String home = System.getProperty("user.home");
    new File(home + "/logs").mkdir();
    new File(home + "/logs/replay").mkdir();
    File logFile = new File(home + "/logs/replay", filename);
    try (RandomAccessFile file = new RandomAccessFile(logFile, "r")) {
      //Get file channel in read-only mode
      FileChannel fileChannel = file.getChannel();
      long start=0;
      long offset=4;

      while (true) {
        //Get direct byte buffer access using channel.map() operation
        MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, start,
            4);
        int lineLength = buffer.getInt();

        MappedByteBuffer contentBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, start+4,
            lineLength+start);

        start=4+lineLength;

        byte[] output = new byte[contentBuffer.limit()];
        contentBuffer.get(output);

        logger.log(Level.INFO, new String(output, StandardCharsets.UTF_8));

      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
