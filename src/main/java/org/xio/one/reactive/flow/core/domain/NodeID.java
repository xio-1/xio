package org.xio.one.reactive.flow.core.domain;

import java.util.Random;

public class NodeID {

  private static long nodeID = new Random().nextLong();

  public static long getNodeID() {
    return nodeID;
  }

  public static void setNodeID(long nodeID) {
    NodeID.nodeID = nodeID;
  }
}
