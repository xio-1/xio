package org.xio.one.reactive.http.weeio.internal.domain;

import java.util.UUID;

public class EventNodeID {

  private static String nodeID = UUID.randomUUID().toString();

  public static String getNodeID() {
    return nodeID;
  }

  public static void setNodeID(String nodeID) {
    EventNodeID.nodeID = nodeID;
  }
}
