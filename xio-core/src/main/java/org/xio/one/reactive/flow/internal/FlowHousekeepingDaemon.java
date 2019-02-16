package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Removes seen dead domain from the contents store
 */
public class FlowHousekeepingDaemon implements Runnable {

  Logger logger = Logger.getLogger(FlowHousekeepingDaemon.class.getCanonicalName());

  @Override
  public void run() {
    logger.log(Level.INFO, "Housekeeping daemon has started");
    try {
      //while any flow is active keep going
      while (Flow.numActiveFlows() > 0) {
        Flow.allFlows().parallelStream().forEach(n -> {
          if (!n.hasEnded())
            n.housekeep();
        });
        Thread.sleep(1000);
      }
    } catch (Exception e) {
      if (Flow.numActiveFlows() > 0)
        logger.log(Level.SEVERE, "Flow housekeeping was interrupted", e);
    }
    logger.log(Level.INFO, "Flow housekeeping has stopped");
  }
}
