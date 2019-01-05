package org.xio.one.reactive.http.weeio.internal.domain;

import java.util.Random;

public class EventNodeID {

  private static long nodeID = new Random().nextLong();

  public static long getNodeID() {
    return nodeID;
  }

  public static void setNodeID(long nodeID) {
    EventNodeID.nodeID = nodeID;
  }
}
