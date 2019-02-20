package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Removes seen dead domain from the contents store
 */
public class FlowHousekeepingTask implements Runnable {

  Logger logger = Logger.getLogger(FlowHousekeepingTask.class.getCanonicalName());

  @Override
  public void run() {
    try {
      if (Flow.numActiveFlows() > 0) {
        Flow.allFlows().parallelStream().forEach(Flow::housekeep);
      }
    } catch (Exception e) {
      if (Flow.numActiveFlows() > 0)
        logger.log(Level.WARNING, "Flow housekeeping was interrupted", e);
    }
  }

}
