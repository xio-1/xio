/*
 * Execute.java
 *
 * Author Xio
 */

package org.xio.one.reactive.flow.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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
      subscriptionsThreadPoolexec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1,new XIOThreadFactory("flow"));
    else if (subscriptionsThreadPoolexec.isShutdown() || subscriptionsThreadPoolexec.isTerminated())
      subscriptionsThreadPoolexec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1, new XIOThreadFactory("flow"));
    return subscriptionsThreadPoolexec;
  }

  public static synchronized ExecutorService subscribersThreadPoolInstance() {
    if (computeThreadPoolexec == null || computeThreadPoolexec.isShutdown() || computeThreadPoolexec
            .isTerminated())
      computeThreadPoolexec =
              Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() +1,new XIOThreadFactory("subscriber"));
    return computeThreadPoolexec;
  }

  public static synchronized ExecutorService controlFlowThreadPoolInstance() {
    if (itemLoopThreadPoolexec == null)
      itemLoopThreadPoolexec = Executors.newFixedThreadPool(3, new XIOThreadFactory("boss"));
    else if (itemLoopThreadPoolexec.isShutdown() || itemLoopThreadPoolexec.isTerminated())
      itemLoopThreadPoolexec = Executors.newFixedThreadPool(3);
    return itemLoopThreadPoolexec;
  }


  public static synchronized ExecutorService ioThreadPoolInstance() {
    if (ioThreadPoolexec == null || ioThreadPoolexec.isShutdown() || ioThreadPoolexec
        .isTerminated())
      ioThreadPoolexec = Executors.newFixedThreadPool(
          (int) Math.round(Runtime.getRuntime().availableProcessors() * 250), new XIOThreadFactory("io"));
    return ioThreadPoolexec;
  }

  private static class XIOThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    XIOThreadFactory() {
      SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() :
              Thread.currentThread().getThreadGroup();
      namePrefix = "xio-" +
              poolNumber.getAndIncrement() +
              "-thread-";
    }

    XIOThreadFactory(String name) {
      SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() :
              Thread.currentThread().getThreadGroup();
      namePrefix = "xio-" + name +
              "-thread-";
    }


    public Thread newThread(Runnable r) {
      Thread t = new Thread(group, r,
              namePrefix + threadNumber.getAndIncrement(),
              0);
      if (t.isDaemon())
        t.setDaemon(false);
      if (t.getPriority() != Thread.NORM_PRIORITY)
        t.setPriority(Thread.NORM_PRIORITY);
      return t;
    }
  }

}
