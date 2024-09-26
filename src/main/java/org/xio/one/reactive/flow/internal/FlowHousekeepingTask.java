package org.xio.one.reactive.flow.internal;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.XIOService;

/**
 * Removes seen dead domain from the getSink store
 */
public class FlowHousekeepingTask implements Runnable {

  Logger logger = Logger.getLogger(FlowHousekeepingTask.class.getCanonicalName());

  @Override
  public void run() {
    try {
      if (Flow.numActiveFlows() > 0 || XIOService.isRunning()) {
        Flow.allFlows().parallelStream().forEach(Flow::housekeep);
      }
    } catch (Exception e) {
      if (Flow.numActiveFlows() > 0) {
        logger.log(Level.WARNING, "Flow housekeeping was interrupted", e);
      }
    }
  }

}
