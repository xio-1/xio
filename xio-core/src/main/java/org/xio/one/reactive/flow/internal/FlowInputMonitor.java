package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.XIOService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gets all the input from the Xio.contents.domain itemStream and persists it to the contents store
 */
public class FlowInputMonitor implements Runnable {

  Logger logger = Logger.getLogger(FlowInputMonitor.class.getCanonicalName());

  @Override
  public void run() {
    logger.log(Level.INFO, "Flow input monitor has started");
    try {
      //while any flow is active keep going
      while (Flow.numActiveFlows() > 0 || XIOService.isRunning()) {
        Flow.allFlows().parallelStream().forEach(n -> {
          if (!n.hasEnded())
            n.acceptAll();
        });
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Flow input monitor was interrupted", e);
      Flow.allFlows().forEach(f->f.close(false));
    }
    logger.log(Level.INFO, "Flow input has monitor stopped");
  }
}
