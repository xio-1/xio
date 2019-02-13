package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.util.InternalExecutors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Gets all the input from the Xio.contents.domain itemStream and persists it to the contents store
 */
public class FlowSubscriptionMonitor implements Runnable {

    Logger logger = Logger.getLogger(FlowSubscriptionMonitor.class.getCanonicalName());

    @Override
    public void run() {
        logger.log(Level.INFO, "Flow Subscription monitor has started");
        try {

            //while any flow is active keep going
            while (Flow.numActiveFlows() > 0) {
                //subscribe to any dirty flow
                ArrayList<Callable<Boolean>> callables = new ArrayList<>();

                Flow.allFlows().stream().filter(f->!f.hasEnded()).map(itemStream -> itemStream.newSubscriptionTask()).forEach(f->callables.add(f));

                if (callables.size()==0) {
                    //sleep if nothing to do
                    Thread.sleep(1);
                }
                else {

                    List<Future<Boolean>> result = InternalExecutors.subscriptionsThreadPoolInstance().invokeAll(callables);

                    Optional<Boolean> anyexecuted= result.stream().map(booleanFuture -> {
                        try {
                            return booleanFuture.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            return true;
                        }
                    }).filter(Boolean::booleanValue).findFirst();
                    //if nothing was processed then sleep
                    if (anyexecuted.isEmpty()) {
                        Thread.sleep(1);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Flow Subscription monitor was interrupted", e);
        }
        logger.log(Level.INFO, "Flow Subscription monitor stopped");
    }
}