package org.xio.one.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.domain.flow.ItemFlow;

import java.util.logging.Logger;

import static org.xio.one.reactive.flow.Flow.anItemFlow;

public class XIOServiceTest {

  final Logger logger = Logger.getLogger(XIOService.class.getCanonicalName());

  @BeforeClass
  public static void setup() {
    XIOService.start();
  }

  @Test
  public void HelloWorld1ShouldRunOnBossThreads() {
    ItemFlow<String, String> asyncFlow = anItemFlow("HelloWorldFlow");
    asyncFlow.publish().doOnNext(i->logger.info(i.value())).subscribe();
    asyncFlow.putItem("Hello World!!!");
    asyncFlow.close(true);
  }

  @Test
  public void HelloWorld2ShouldRunOnBossThreads() {
    ItemFlow<String, String> asyncFlow = anItemFlow("HelloWorldFlow");
    asyncFlow.publish().doOnNext(i->logger.info(i.value())).subscribe();
    asyncFlow.putItem("Hello World Again!!!");
    asyncFlow.close(true);
  }

  @AfterClass
  public static void tearDown() {
    XIOService.stop();
  }

}
