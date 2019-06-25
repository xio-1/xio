package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.internal.FlowHousekeepingTask;
import org.xio.one.reactive.flow.internal.FlowInputMonitor;
import org.xio.one.reactive.flow.internal.FlowSubscriptionMonitor;
import org.xio.one.reactive.flow.util.InternalExecutors;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@SupportedAnnotationTypes("org.xio.one.reactive.flow.annotations.EnableXIO")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class XIOService {

  private static final Object lock = new Object();
  private static XIOService xioBoss;
  private static Logger logger;
  private static String banner =
      "\n" + "\n" + " /**   /** /******  /****** \n" + "| **  / **|_  **_/ " + "/**__  **\n"
          + "|  **/ **/  | **  | **  \\ **\n" + " \\  ****/   | **  | **  | **\n"
          + "  >**  **   | **  | **  | **\n" + " /**/\\  **  | **  | **  | **\n"
          + "| **  \\ ** /******|  ******/\n" + "|__/  |__/|______/ \\______/ \n"
          + "                            \n";

  static {
    try {
      InputStream stream = XIOService.class.getResourceAsStream(
          "/logger.properties");
      if (stream != null)
        LogManager.getLogManager().readConfiguration(stream);
    } catch (Exception e) {
    } finally {
      logger = Logger.getLogger(Flow.class.getName());
    }
  }

  private Future flowInputMonitorFuture;
  private Future flowSubscriptionMonitorFuture;
  private Future flowHousekeepingDaemonFuture;

  private XIOService(Future<?> submit, Future<?> submit1, Future<?> submit2) {
    flowInputMonitorFuture = submit;
    flowSubscriptionMonitorFuture = submit1;
    flowHousekeepingDaemonFuture = submit2;
  }

  public static void start() {
    synchronized (lock) {
      if (xioBoss == null) {
        logger.info(banner);
        xioBoss = new XIOService(
            InternalExecutors.controlFlowThreadPoolInstance().submit(new FlowInputMonitor()),
            InternalExecutors.controlFlowThreadPoolInstance().submit(new FlowSubscriptionMonitor()),
            InternalExecutors.schedulerThreadPoolInstance()
                .scheduleWithFixedDelay(new FlowHousekeepingTask(), 1, 1, TimeUnit.SECONDS));
        try {
          //give boss threads a chance to start correctly
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        logger.info("XIO loaded successfully");
      }
    }

  }

  public static void stop() {
    synchronized (lock) {
      if (xioBoss != null) {
        XIOService oldBoss = xioBoss;
        xioBoss = null;
        try {
          //give in-flight data a chance to end correctly
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        oldBoss.getFlowInputMonitorFuture().cancel(true);
        oldBoss.getFlowSubscriptionMonitorFuture().cancel(true);
        oldBoss.getFlowHousekeepingDaemonFuture().cancel(true);
        logger.info("XIO stopped");
      }
    }
  }

  public static boolean isRunning() {
    synchronized (lock) {
      return xioBoss != null;
    }
  }

  private Future getFlowInputMonitorFuture() {
    return flowInputMonitorFuture;
  }

  private Future getFlowSubscriptionMonitorFuture() {
    return flowSubscriptionMonitorFuture;
  }

  private Future getFlowHousekeepingDaemonFuture() {
    return flowHousekeepingDaemonFuture;
  }

}
