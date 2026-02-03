package org.xio.one.reactive.flow.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.util.InternalExecutors;

/**
 * Gets allItems the input from the Xio.getSink.domain itemStream and persists it to the getSink
 * store
 */
public class SubscriptionTaskDispatcher implements Runnable {
    private int countDown = 10;
    final Thread parkedThread = Thread.currentThread();
    Logger logger = Logger.getLogger(SubscriptionTaskDispatcher.class.getCanonicalName());

    public void unpark() {
        LockSupport.unpark(parkedThread);
    }

    @Override
    public void run() {
        logger.log(Level.INFO, "Flow Subscription monitor has started");

        //while any flow is active keep going
        while (Flow.numActiveFlows() > 0 || XIOService.isRunning()) {
            //publishTo to any dirty flow
            try {
                ArrayList<Callable<Boolean>> callables = new ArrayList<>();

                Flow.allFlows().stream().filter(f -> !f.hasEnded())
                        .map(Flow::newSubscriptionTask).forEach(callables::add);

                if (callables.isEmpty()) {
                    //sleep if nothing to do
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {

                    }
                } else {
                    List<Future<Boolean>> result;
                    result = InternalExecutors.flowInputTaskThreadPoolInstance().invokeAll(callables);
                    while (result.stream().anyMatch(p -> !p.isDone())) {
                        Thread.sleep(1);
                    }
                }
            } catch (Exception e) {
                countDown--;
                if (countDown == 0) {
                    logger.log(Level.SEVERE,
                            "Exiting : Cannot continue Flow Subscription monitor exceeded retry count " + e);
                    System.exit(-1);
                } else logger.log(Level.WARNING,
                        "Flow Subscription monitor experienced an unexpected error " + e);
            }
        }

        logger.log(Level.INFO, "Flow Subscription monitor stopped");
    }
}
