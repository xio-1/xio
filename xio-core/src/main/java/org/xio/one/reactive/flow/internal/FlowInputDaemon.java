package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;

import java.util.concurrent.locks.LockSupport;
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
      while (Flow.allFlows().size()>0) {
        Flow.allFlows().values().parallelStream().forEach(s->s.acceptAll());
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Flow input was interrupted", e);
    }
    logger.log(Level.INFO, "Flow input stopped");
  }
}
