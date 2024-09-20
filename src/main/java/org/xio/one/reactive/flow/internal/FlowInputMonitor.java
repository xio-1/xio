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
public class FlowInputMonitor implements Runnable {

  Logger logger = Logger.getLogger(FlowInputMonitor.class.getCanonicalName());


  public final class FlowInputTask implements Callable<Boolean> {

    Logger logger = Logger.getLogger(Flow.FlowSubscriptionTask.class.getCanonicalName());
    private Flow itemStream;

    public FlowInputTask(Flow itemStream) {

      this.itemStream = itemStream;
    }

    @Override
    public Boolean call() throws Exception {
      return itemStream.acceptAll();
    }
  }

  @Override
  public void run() {
    logger.log(Level.INFO, "Flow input monitor has started");
    try {
      //while any flow is active keep going
      while (XIOService.isRunning()) {
        doAcceptAllStreams();
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Flow input monitor was interrupted", e);
      Flow.allFlows().forEach(f -> f.close(false));
    }
    logger.log(Level.INFO, "Flow input has monitor stopped");
  }

  private void doAcceptAllStreams() throws Exception {

    ArrayList<Callable<Boolean>> callables = new ArrayList<>();

    Flow.allFlows().stream().filter(f -> !f.hasEnded() && f.buffer_size() > 0)
        .map(itemStream -> new FlowInputTask(itemStream)).forEach(f -> callables.add(f));

    try {
      List<Future<Boolean>> result =
              InternalExecutors.flowInputTaskThreadPoolInstance().invokeAll(callables);
      Optional<Boolean> anyexecuted = result.stream().map(booleanFuture -> {
        try {
          return booleanFuture.get();
        } catch (InterruptedException | ExecutionException e) {
          return false;
        }
      }).filter(Boolean::booleanValue).findFirst();
      if (anyexecuted.isEmpty()) {
        LockSupport.parkUntil(Thread.currentThread(), System.currentTimeMillis() + 100);
      }

    } catch (InterruptedException e) {
      System.exit(-1);
    }

  }
}
