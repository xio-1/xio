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
 *
 * @author Xio
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

  public static synchronized ExecutorService itemLoopThreadPoolInstance() {
    if (itemLoopThreadPoolexec == null)
      itemLoopThreadPoolexec = Executors.newCachedThreadPool();
    else if (itemLoopThreadPoolexec.isShutdown() || itemLoopThreadPoolexec.isTerminated())
      itemLoopThreadPoolexec = Executors.newCachedThreadPool();
    return itemLoopThreadPoolexec;
  }


  public static synchronized ExecutorService computeThreadPoolInstance() {
    if (computeThreadPoolexec == null || computeThreadPoolexec.isShutdown() || computeThreadPoolexec
        .isTerminated())
      computeThreadPoolexec =
          Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    return computeThreadPoolexec;
  }


  public static synchronized ExecutorService ioThreadPoolInstance() {
    if (ioThreadPoolexec == null || ioThreadPoolexec.isShutdown() || ioThreadPoolexec
        .isTerminated())
      ioThreadPoolexec =
          Executors.newFixedThreadPool(1024);
    return ioThreadPoolexec;
  }

}
