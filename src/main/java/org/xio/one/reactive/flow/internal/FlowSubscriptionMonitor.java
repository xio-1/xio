package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gets allItems the input from the Xio.getSink.domain itemStream and persists it to the getSink store
 */
public class FlowSubscriptionMonitor implements Runnable {

  Logger logger = Logger.getLogger(FlowSubscriptionMonitor.class.getCanonicalName());

  final Thread parkedThread = Thread.currentThread();

  public void unpark() {
    LockSupport.unpark(parkedThread);
  }

  @Override
  public void run() {
    logger.log(Level.INFO, "Flow Subscription monitor has started");
    try {

      //while any flow is active keep going
      while (Flow.numActiveFlows() > 0 || XIOService.isRunning()) {
        //publishTo to any dirty flow
        ArrayList<Callable<Boolean>> callables = new ArrayList<>();

        Flow.allFlows().stream().filter(f -> !f.hasEnded())
            .map(itemStream -> itemStream.newSubscriptionTask()).forEach(f -> callables.add(f));

        if (callables.size() == 0) {
          //sleep if nothing to do
          Thread.sleep(100);
        } else {
          List<Future<Boolean>> result =
              InternalExecutors.flowInputTaskThreadPoolInstance().invokeAll(callables);

          Optional<Boolean> anyexecuted = result.stream().map(booleanFuture -> {
            try {
              return booleanFuture.get();
            } catch (InterruptedException | ExecutionException e) {
              e.printStackTrace();
              return true;
            }
          }).filter(Boolean::booleanValue).findFirst();
          //if nothing was processed then sleep
          if (anyexecuted.isEmpty()) {
            LockSupport.parkUntil(Thread.currentThread(),System.currentTimeMillis()+100);
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Flow Subscription monitor was interrupted", e);
    }
    logger.log(Level.INFO, "Flow Subscription monitor stopped");
  }
}
