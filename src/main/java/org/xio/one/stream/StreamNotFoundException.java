package org.xio.one.stream;

/**
 * Created by Admin on 21/09/2014.
 */
public class StreamNotFoundException extends Exception {
  String msg;

  public StreamNotFoundException(String msg) {
    this.msg = msg;
  }

}
