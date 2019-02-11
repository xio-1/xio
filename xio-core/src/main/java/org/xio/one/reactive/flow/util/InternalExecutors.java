/*
 * Execute.java
 *
 * Author Xio
 */

package org.xio.one.reactive.flow.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wraps singleton methods arround Executors created cached thread pools
 */
public class InternalExecutors {

  private static ExecutorService subscriptionsThreadPoolexec;
  private static ExecutorService itemLoopThreadPoolexec;
  private static ExecutorService computeThreadPoolexec;
  private static ExecutorService ioThreadPoolexec;

  /**
   * Gets an instance anItemFlow ExecutorService for the application XIO threadpool
   *
   * @return
   */
  public static synchronized ExecutorService subscriptionsThreadPoolInstance() {
    if (subscriptionsThreadPoolexec == null)
      subscriptionsThreadPoolexec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    else if (subscriptionsThreadPoolexec.isShutdown() || subscriptionsThreadPoolexec.isTerminated())
      subscriptionsThreadPoolexec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    return subscriptionsThreadPoolexec;
  }

  public static synchronized ExecutorService subscribersThreadPoolInstance() {
    if (computeThreadPoolexec == null || computeThreadPoolexec.isShutdown() || computeThreadPoolexec
            .isTerminated())
      computeThreadPoolexec =
              Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    return computeThreadPoolexec;
  }

  public static synchronized ExecutorService controlFlowThreadPoolInstance() {
    if (itemLoopThreadPoolexec == null)
      itemLoopThreadPoolexec = Executors.newFixedThreadPool(3);
    else if (itemLoopThreadPoolexec.isShutdown() || itemLoopThreadPoolexec.isTerminated())
      itemLoopThreadPoolexec = Executors.newFixedThreadPool(3);
    return itemLoopThreadPoolexec;
  }


  public static synchronized ExecutorService ioThreadPoolInstance() {
    if (ioThreadPoolexec == null || ioThreadPoolexec.isShutdown() || ioThreadPoolexec
        .isTerminated())
      ioThreadPoolexec = Executors.newFixedThreadPool(
          (int) Math.round(Runtime.getRuntime().availableProcessors() * 1.2 / 0.02));
    return ioThreadPoolexec;
  }

}
