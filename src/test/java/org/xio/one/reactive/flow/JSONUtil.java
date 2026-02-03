package org.xio.one.reactive.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JSONUtil {
  static Logger logger = Logger.getLogger(JSONUtil.class.getCanonicalName());
  static ObjectMapper mapper = new ObjectMapper();

  public static String toJSONString(Object value) throws IOException {
    try {
      mapper = new ObjectMapper();
      return mapper.writeValueAsString(value);
    } catch (IOException ioe) {
      logger.log(Level.SEVERE, "json format exception", ioe);
      throw new IOException(ioe);
    }
  }

  public static <T> T fromJSONString(String jsonValue, Class<T> to) throws IOException {
    mapper = new ObjectMapper();
    return mapper.readValue(jsonValue, to);
  }

}
