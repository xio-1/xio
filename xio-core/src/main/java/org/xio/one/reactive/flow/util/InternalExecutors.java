/*
 * Execute.java
 *
 * Author Xio
 */

package org.xio.one.reactive.flow.util;

import org.xio.one.reactive.flow.internal.FlowInputDaemon;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Wraps singleton methods arround Executors created cached thread pools
 */
public class InternalExecutors {

  private static ExecutorService cachedThreadPoolexec;
  private static ExecutorService itemLoopThreadPoolexec;
  private static ExecutorService computeThreadPoolexec;
  private static ExecutorService ioThreadPoolexec;

  /**
   * Gets an instance anItemFlow ExecutorService for the application JETI threadpool
   *
   * @return
   */
  public static synchronized ExecutorService subscriberCachedThreadPoolInstance() {
    if (cachedThreadPoolexec == null)
      cachedThreadPoolexec = Executors.newCachedThreadPool();
    else if (cachedThreadPoolexec.isShutdown() || cachedThreadPoolexec.isTerminated())
      cachedThreadPoolexec = Executors.newCachedThreadPool();
    return cachedThreadPoolexec;
  }

  public static synchronized ExecutorService flowControlThreadPoolInstance() {
    if (itemLoopThreadPoolexec == null)
      itemLoopThreadPoolexec = Executors.newFixedThreadPool(2);
    else if (itemLoopThreadPoolexec.isShutdown() || itemLoopThreadPoolexec.isTerminated())
      itemLoopThreadPoolexec = Executors.newFixedThreadPool(2);
    return itemLoopThreadPoolexec;
  }


  public static synchronized ExecutorService computeThreadPoolInstance() {
    if (computeThreadPoolexec == null || computeThreadPoolexec.isShutdown() || computeThreadPoolexec
        .isTerminated())
      computeThreadPoolexec =
          Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    return computeThreadPoolexec;
  }


  public static synchronized ExecutorService ioThreadPoolInstance() {
    if (ioThreadPoolexec == null || ioThreadPoolexec.isShutdown() || ioThreadPoolexec
        .isTerminated())
      ioThreadPoolexec = Executors.newFixedThreadPool(
          (int) Math.round(Runtime.getRuntime().availableProcessors() * 1.2 / 0.02));
    return ioThreadPoolexec;
  }

}
