package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.FlowContents;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gets all the input from the Xio.contents.domain itemStream and persists it to the contents store
 */
public class FlowInputDaemon implements Runnable {

  Logger logger = Logger.getLogger(FlowInputDaemon.class.getCanonicalName());

  @Override
  public void run() {
    logger.log(Level.INFO, "Flow input daemon has started");
    try {
      //while any flow is active keep going
      while (Flow.numActiveFlows()>0) {
        Flow.allFlows().parallelStream().forEach(n -> {
          if (!n.hasEnded())
            n.acceptAll();
        });
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Flow input was interrupted", e);
    }
    logger.log(Level.INFO, "Flow input has stopped");
  }
}
