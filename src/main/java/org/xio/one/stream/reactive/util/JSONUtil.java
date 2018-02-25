package org.xio.one.stream.reactive.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JSONUtil {

  static ObjectMapper mapper = new ObjectMapper();

  public static String toJSONString(Object value) {
    try {
      mapper = new ObjectMapper();
      return mapper.writeValueAsString(value);
    } catch (IOException ioe) {
      return "{\"exception\":\"" + "json format exception" + "\"}";
    }
  }

  public static <T> T fromJSONString(String jsonValue, Class<T> to) throws IOException {
    mapper = new ObjectMapper();
    return mapper.readValue(jsonValue, to);
  }

}
