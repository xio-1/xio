package org.xio.one.reactive.flow.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.util.InternalExecutors;

/**
 * Gets allItems the input from the Xio.getSink.domain itemStream and persists it to the getSink
 * store
 */
public class FlowInputMonitor implements Runnable {

    private Logger logger = Logger.getLogger(FlowInputMonitor.class.getCanonicalName());

    @Override
    public void run() {
        logger.log(Level.INFO, "Flow input monitor has started");
        try {
            //while any flow is active keep going
            while (XIOService.isRunning()) {
                doAcceptAllStreams();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exiting: an unexpected exception occurred with the Flow input monitor.", e);
            Flow.allFlows().forEach(f -> f.close(false));
        }
        logger.log(Level.INFO, "Flow input has monitor stopped");
    }

    private void doAcceptAllStreams() {
        int countDown = 10;
        ArrayList<Callable<Boolean>> callables = new ArrayList<>();

        Flow.allFlows().stream().filter(f -> !f.hasEnded() && f.buffer_size() > 0).map(itemStream -> new FlowInputTask(itemStream)).forEach(f -> callables.add(f));
        try {
            if (!callables.isEmpty()) {

                List<Future<Boolean>> result =
                        InternalExecutors.microFlowTaskThreadPoolInstance().invokeAll(callables);

                while (result.stream().anyMatch(r -> !r.isDone())) {
                    Thread.sleep(1);
                }

                List<Boolean> executed = result.stream().map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
            } else Thread.sleep(1);
        } catch (InterruptedException | RuntimeException e) {
            handleException(e, countDown);
        }
    }

    private void handleException(Exception e, int countDown) {
        countDown--;
        if (countDown == 0) {
            logger.log(Level.SEVERE,
                    "Cannot continue Flow input monitor exceeded retry count " + e);
            System.exit(-1);
        } else logger.log(Level.SEVERE,
                "THe flow input monitor experienced an unexpected error " + e.getMessage() + " this will result in data loss for a flow that does not use completable/promise backed submission ");
    }


    public final class FlowInputTask implements Callable<Boolean> {

        private final Flow itemStream;
        Logger logger = Logger.getLogger(Flow.FlowSubscriptionTask.class.getCanonicalName());

        public FlowInputTask(Flow itemStream) {
            this.itemStream = itemStream;
        }

        @Override
        public Boolean call() throws Exception {
            return itemStream.acceptAll();
        }
    }
}
