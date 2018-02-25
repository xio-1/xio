/*
 * Execute.java
 *
 * Author Xio
 */

package org.xio.one.stream.reactive.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wraps singleton methods arround Executors created cached thread pools
 *
 * @author Xio
 */
public class AsyncStreamExecutor {

  private static ExecutorService cachedThreadPoolexec;
  private static ExecutorService eventLoopThreadPoolexec;
  private static ExecutorService fixedThreadPoolexec;

  /**
   * Gets an instance of ExecutorService for the application JETI threadpool
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

  public static synchronized ExecutorService eventLoopThreadPoolInstance() {
    if (eventLoopThreadPoolexec == null)
      eventLoopThreadPoolexec = Executors.newCachedThreadPool();
    else if (eventLoopThreadPoolexec.isShutdown() || eventLoopThreadPoolexec.isTerminated())
      eventLoopThreadPoolexec = Executors.newCachedThreadPool();
    return eventLoopThreadPoolexec;
  }


  public static synchronized ExecutorService fixedThreadPoolInstance() {
    if (fixedThreadPoolexec == null || fixedThreadPoolexec.isShutdown() || fixedThreadPoolexec
        .isTerminated())
      fixedThreadPoolexec =
          Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    return fixedThreadPoolexec;
  }

}
