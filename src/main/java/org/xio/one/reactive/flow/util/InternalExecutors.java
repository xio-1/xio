/*
 * Execute.java
 *
 * Author Xio
 */

package org.xio.one.reactive.flow.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps singleton methods arround Executors created cached thread pools
 */
public class InternalExecutors {

  private static ExecutorService flowInputThreadPoolexec;
  private static ExecutorService bossThreadPoolexec;
  private static ExecutorService computeThreadPoolexec;
  private static ExecutorService ioThreadPoolexec;
  private static ScheduledExecutorService schedulerThreadPoolexec;

  /**
   * Gets an instance anItemFlow ExecutorService for the application XIO threadpool
   *
   * @return
   */
  public static synchronized ExecutorService flowInputTaskThreadPoolInstance() {
    if (flowInputThreadPoolexec == null)
      flowInputThreadPoolexec = Executors
          .newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1,
              new XIOThreadFactory("flow"));
    else if (flowInputThreadPoolexec.isShutdown() || flowInputThreadPoolexec.isTerminated())
      flowInputThreadPoolexec = Executors
          .newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1,
              new XIOThreadFactory("flow"));
    return flowInputThreadPoolexec;
  }

  public static synchronized ScheduledExecutorService schedulerThreadPoolInstance() {
    if (schedulerThreadPoolexec == null || schedulerThreadPoolexec.isShutdown()
        || schedulerThreadPoolexec.isTerminated())
      schedulerThreadPoolexec = Executors.newScheduledThreadPool(1,
          new XIOThreadFactory("cleaner", Thread.NORM_PRIORITY + 2, false));
    return schedulerThreadPoolexec;
  }

  public static synchronized ExecutorService subscribersTaskThreadPoolInstance() {
    if (computeThreadPoolexec == null || computeThreadPoolexec.isShutdown() || computeThreadPoolexec
        .isTerminated())
      computeThreadPoolexec = Executors
          .newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1,
              new XIOThreadFactory("subscriber", Thread.NORM_PRIORITY + 1, false));
    return computeThreadPoolexec;
  }

  public static synchronized ExecutorService daemonThreadPoolInstance() {
    if (bossThreadPoolexec == null || bossThreadPoolexec.isShutdown()
        || bossThreadPoolexec.isTerminated())
      bossThreadPoolexec = Executors
          .newFixedThreadPool(2, new XIOThreadFactory("boss", Thread.NORM_PRIORITY + 1, true));
    return bossThreadPoolexec;
  }


  public static synchronized ExecutorService ioThreadPoolInstance() {
    if (ioThreadPoolexec == null || ioThreadPoolexec.isShutdown() || ioThreadPoolexec
        .isTerminated())
      ioThreadPoolexec = Executors
          .newFixedThreadPool((int) Math.round(Runtime.getRuntime().availableProcessors() * 250),
              new XIOThreadFactory("io"));
    return ioThreadPoolexec;
  }

  private static class XIOThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private int priority = Thread.NORM_PRIORITY;
    private boolean areDeamonThreads = false;

    XIOThreadFactory() {
      SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      namePrefix = "xio-" + poolNumber.getAndIncrement() + "-thread-";
    }

    XIOThreadFactory(String name) {
      SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      namePrefix = "xio-" + name + "-thread-";
    }


    XIOThreadFactory(String name, int priority, boolean areDeamonThreads) {
      this.priority = priority;
      this.areDeamonThreads = areDeamonThreads;
      SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      namePrefix = "xio-" + name + "-thread-";
    }

    public Thread newThread(Runnable r) {
      Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
      t.setDaemon(this.areDeamonThreads);
      t.setPriority(this.priority);
      return t;
    }
  }

}
