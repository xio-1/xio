package org.xio.one.reactive.flow.domain;

import java.util.Random;

public class NodeID {

  private static long nodeID = Math.abs(new Random().nextLong());

  public static long getNodeID() {
    return nodeID;
  }

}
