package org.xio.one.reactive.flow;

import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.xio.one.reactive.flow.internal.FlowHousekeepingTask;
import org.xio.one.reactive.flow.internal.FlowInputMonitor;
import org.xio.one.reactive.flow.internal.FlowSubscriptionMonitor;
import org.xio.one.reactive.flow.util.InternalExecutors;


public class XIOService {

  private static final Object lock = new Object();
  private static final String banner =
      "\n" + "\n" + " /**   /** /******  /****** \n" + "| **  / **|_  **_/ " + "/**__  **\n"
          + "|  **/ **/  | **  | **  \\ **\n" + " \\  ****/   | **  | **  | **\n"
          + "  >**  **   | **  | **  | **\n" + " /**/\\  **  | **  | **  | **\n"
          + "| **  \\ ** /******|  ******/\n" + "|__/  |__/|______/ \\______/ \n"
          + "                            \n";
  private static XIOService xioBoss;
  private static Logger logger;
  private final Future flowInputMonitorFuture;
  private final Future flowSubscriptionMonitorFuture;
  private final Future flowHousekeepingDaemonFuture;
  private FlowInputMonitor flowInputMonitor;
  private FlowSubscriptionMonitor flowSubscriptionMonitor;

  private XIOService(Future<?> submit, Future<?> submit1, Future<?> submit2,
      FlowInputMonitor flowInputMonitor, FlowSubscriptionMonitor flowSubscriptionMonitor) {
    this.flowInputMonitorFuture = submit;
    this.flowSubscriptionMonitorFuture = submit1;
    this.flowHousekeepingDaemonFuture = submit2;
    this.flowInputMonitor = flowInputMonitor;
    this.flowSubscriptionMonitor = flowSubscriptionMonitor;
  }

  private static Logger loadLogger() {
    try {
      InputStream stream = XIOService.class.getResourceAsStream("/logger.properties");
      if (stream != null) {
        LogManager.getLogManager().readConfiguration(stream);
      }
      return Logger.getLogger(Flow.class.getName() + System.currentTimeMillis());
    } catch (Exception e) {
      return Logger.getLogger(Flow.class.getName() + System.currentTimeMillis());
    }

  }

  public static void start() {
    boolean exit = false;
    while (!exit) {
      synchronized (lock) {
        if (xioBoss == null) {
          logger = loadLogger();
          logger.info(banner);
          FlowInputMonitor flowInputMonitor = new FlowInputMonitor();
          FlowSubscriptionMonitor flowSubscriptionMonitor = new FlowSubscriptionMonitor();
          xioBoss = new XIOService(
              InternalExecutors.daemonThreadPoolInstance().submit(flowInputMonitor),
              InternalExecutors.daemonThreadPoolInstance().submit(flowSubscriptionMonitor),
              InternalExecutors.schedulerThreadPoolInstance()
                  .scheduleWithFixedDelay(new FlowHousekeepingTask(), 1, 1,
                      TimeUnit.SECONDS),
              flowInputMonitor, flowSubscriptionMonitor);
          try {
            //give boss threads a chance to start correctly
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          logger.info("XIO loaded successfully");
          try {
            Thread.class.getDeclaredMethod("startVirtualThread", Runnable.class);
            logger.log(Level.WARNING, "\033[41m*** XIO is using Virtual Threads For Subscribers ***]\033[m");
          } catch (NoSuchMethodException e) {
          }
          exit = true;
        } else {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }

  }

  public static XIOService getXioBoss() {
    return xioBoss;
  }

  public static void stop() {
    synchronized (lock) {
      if (xioBoss != null) {
        XIOService oldBoss = xioBoss;
        try {
          //give in-flight data a chance to end correctly
          Thread.sleep(2000);
          Flow.allFlows().stream().forEach(f -> {
            f.close(false);
            f.getSink().itemStoreContents.clear();
          });
          Flow.allFlows().clear();
          oldBoss.getFlowInputMonitorFuture().cancel(true);
          oldBoss.getFlowSubscriptionMonitorFuture().cancel(true);
          oldBoss.getFlowHousekeepingDaemonFuture().cancel(true);
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        logger.info("XIO stopped");
        xioBoss = null;
        oldBoss = null;
      } else {
        throw new RuntimeException("No XIO Boss");
      }
    }
  }

  public static boolean isRunning() {
    synchronized (lock) {
      return xioBoss != null;
    }
  }

  private Future getFlowInputMonitorFuture() {
    return this.flowInputMonitorFuture;
  }

  private Future getFlowSubscriptionMonitorFuture() {
    return this.flowSubscriptionMonitorFuture;
  }

  private Future getFlowHousekeepingDaemonFuture() {
    return this.flowHousekeepingDaemonFuture;
  }

  public FlowInputMonitor getFlowInputMonitor() {
    return this.flowInputMonitor;
  }

  public void setFlowInputMonitor(FlowInputMonitor flowInputMonitor) {
    this.flowInputMonitor = flowInputMonitor;
  }

  public FlowSubscriptionMonitor getFlowSubscriptionMonitor() {
    return this.flowSubscriptionMonitor;
  }

  public void setFlowSubscriptionMonitor(FlowSubscriptionMonitor flowSubscriptionMonitor) {
    this.flowSubscriptionMonitor = flowSubscriptionMonitor;
  }


}
