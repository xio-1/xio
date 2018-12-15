package org.xio.one.reactive.http.wee;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.domain.ItemFlowable;
import org.xio.one.reactive.flow.subscriber.StreamItemSubscriber;
import org.xio.one.reactive.http.wee.event.platform.api.ApiBootstrap;
import org.xio.one.reactive.http.wee.event.platform.api.JSONUtil;
import org.xio.one.reactive.http.wee.event.platform.domain.Event;
import org.xio.one.reactive.http.wee.event.platform.domain.EventNodeID;
import org.xio.one.reactive.http.wee.event.platform.service.EventChannel;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

import static io.undertow.Handlers.*;


/**
 * @author Richard Durley
 */
public class WEEServer {

  UndertowJaxrsServer server;
  private static String PING_CHAR_STRING = Character.toString('\u0000');
  private static Logger logger = Logger.getLogger(WEEServer.class.getCanonicalName());

  public static void main(final String[] args) {

    try {
      List<String> argList = Arrays.asList(args);
      WEEServer eventServer = new WEEServer();
      final String channelName;

      if (argList.contains("--h") || argList.contains("--help")) {
        logger.info("Usage Help");
        logger.info("[-n name] : create stream with name, default name is platform");
        logger.info("[-ttl integer] : platform time to live in seconds, default is 0 for forever");
        logger.info("[-s port] start as master server stream publisher on given port");
        logger.info("[-c ipaddress:port] connect to a master server @ipaddress:port");
        logger.info("[-a port] start stream web api on given port");
        logger.info("");
        logger.info("Example EventServer -n mystream -ttl 0 -p 7222 -a 8080");
        System.exit(0);
      }

      if (argList.contains("-n")) {
        channelName = argList.get((argList.indexOf("-n") + 1));
      } else {
        channelName = "events";
      }

      int ttl = 60;
      if (argList.contains("-ttl")) {
        ttl = Integer.parseInt(argList.get((argList.indexOf("-ttl") + 1)));
      }

      EventChannel.channel(channelName);

      logger.info("**** created channel" + channelName + " with ttl seconds " + ttl);

      if (argList.contains("-s")) {
        int serverPort;
        serverPort = Integer.parseInt(argList.get((argList.indexOf("-s") + 1)));
        eventServer.withWebSocketEventServer(channelName, serverPort, ttl);
        logger.info(
            "**** started master server socket @ http://0.0.0.0:" + serverPort + "/" + channelName
                + "/subscribe");
      }

      /*if (argList.contains("-c")) {
        final String clientURL =
            "http://" + argList.get(argList.indexOf("-c") + 1) + "/" + channelName + "/subscribe";

        logger.info("Trying to connect to master node " + clientURL);
        new Thread(() -> {
          try {
            final Xnio xnio = Xnio.getInstance("nio", WEEServer.class.getClassLoader());
            final XnioWorker worker = xnio.createWorker(
                OptionMap.builder().set(Options.WORKER_IO_THREADS, 1)
                    .set(Options.CONNECTION_HIGH_WATER, 10).set(Options.CONNECTION_LOW_WATER, 1)
                    .set(Options.WORKER_TASK_CORE_THREADS, 1)
                    .set(Options.WORKER_TASK_MAX_THREADS, 1).set(Options.TCP_NODELAY, true)
                    .set(Options.CORK, true).getMap());

            boolean connected = false;
            while (!connected) {
              try {
                WebSocketChannel webSocketChannel =
                    eventServer.withWebSocketEventClient(clientURL, channelName, worker);
                if (webSocketChannel != null && webSocketChannel.isOpen())
                  connected = true;
                else
                  System.out.print(".");

              } catch (Exception e) {
                e.getMessage();
              }
            }
          } catch (Exception e) {
          }
        }).start();
      }*/


      int apiport = 8080;
      if (argList.contains("-a")) {
        apiport = Integer.parseInt(argList.get((argList.indexOf("-a") + 1)));
        eventServer.withAPI(ApiBootstrap.class, apiport);
        System.out.println(
            "**** starting web api @ http://0.0.0.0:" + apiport + "/channel/" + channelName);
      }

      logger.info("**** All services are started");

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

  }

  private static String getIPAddress(String interfaceName) {
    try {
      NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
      if (networkInterface != null) {
        Enumeration<InetAddress> inetAddress = networkInterface.getInetAddresses();
        while (inetAddress.hasMoreElements()) {
          InetAddress currentAddress = inetAddress.nextElement();
          if (currentAddress instanceof Inet4Address && !currentAddress.isLoopbackAddress()) {
            return currentAddress.toString().replaceAll("/", "");
          }
        }
      }
    } catch (SocketException e) {

    }
    return null;
  }

  private static String getHostName(String ipaddress) {
    try {
      InetAddress addr = InetAddress.getByName(ipaddress);
      return addr.getHostName();
    } catch (Exception e) {
      return null;
    }
  }

  public WEEServer withAPI(Class application, int port) throws Exception {
    Undertow.Builder serverBuilder =
        Undertow.builder().addHttpListener(port, "0.0.0.0").setWorkerThreads(4);
    server = new UndertowJaxrsServer().start(serverBuilder);
    DeploymentInfo di = server.undertowDeployment(application);
    di.setContextPath("/");
    di.setDeploymentName("org.xio.one.reactive.http.wee");
    server.deploy(di);
    return this;
  }


  /*public WebSocketChannel withWebSocketEventClient(String remoteURL, String eventStreamName,
      XnioWorker worker) throws Exception {


    EventChannel.channel(eventStreamName).flow()
        .addSubscriber(new StreamItemSubscriber<String, Event>() {
                         @Override
                         public void onNext(FlowItem<Event, String> flowItem) throws Throwable {

                         }
                       }

        );
    SubscriberResult<Event[]> subscriberResult =
        (new Subscription<>(eventStream, Subscribers.CollectorSubscriber())).subscribe();
    IoFuture<WebSocketChannel> connection;
    new connection = WebSocketClient
        .connectionBuilder(worker, new DefaultByteBufferPool(true, 32768), new URI(remoteURL))
        .connect(); Thread.currentThread().sleep(1000);
    if (connection.getStatus() == IoFuture.Status.DONE) {
      logger.info(".");
      logger.info("Connected to cluster server@" + remoteURL);
      WebSocketChannel channel = connection.get();
      channel.getReceiveSetter().set(new AbstractReceiveListener() {

        @Override
        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel)
            throws IOException {
          super.onClose(webSocketChannel, channel);
          eventStream.end(true);
        }

        @Override
        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message)
            throws IOException {
          processMessageData(message, eventStream);
        }
      });

      new Thread(() -> {
        while (!eventStream.isEnd() && channel.isOpen()) {
          Event[] events = subscriberResult.getNextAndReset(100, TimeUnit.MILLISECONDS);
          try {
            if (events != null && events.length > 0) {
              String toSend = JSONUtil.toJSONString(events);
              WebSockets.sendTextBlocking("events:" + toSend, channel);
            } else {
              WebSockets.sendTextBlocking(PING_CHAR_STRING, channel);
            }
          } catch (java.io.IOException e) {
            logger.info("b");
          }
          channel.resumeReceives();
        }
      }).start();
      return channel;
    } else {
      connection.cancel();
    }
    return null;

  }*/

  public WEEServer withWebSocketEventServer(String eventStreamName, final int port, int ttl)
      throws IOException {



    final Xnio xnio = Xnio.getInstance("nio", WEEServer.class.getClassLoader());
    final XnioWorker worker = xnio.createWorker(
        OptionMap.builder().set(Options.WORKER_IO_THREADS, 8)
            .set(Options.CONNECTION_HIGH_WATER, 1000000).set(Options.CONNECTION_LOW_WATER, 10)
            .set(Options.WORKER_TASK_CORE_THREADS, 30).set(Options.WORKER_TASK_MAX_THREADS, 30)
            .set(Options.TCP_NODELAY, true).set(Options.CORK, true).getMap());

    Undertow server = Undertow.builder().addHttpListener(port, "0.0.0.0").setWorker(worker)
        .setHandler(path().addPrefixPath("/" + eventStreamName + "/subscribe",
            websocket(new WebSocketConnectionCallback() {

              long count = 0;

              @Override
              public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {

                EventChannel.channel(eventStreamName).flow()
                    .addSubscriber(new StreamItemSubscriber<String, Event>() {
                      @Override
                      public void onNext(FlowItem<Event, String> flowItem) throws Throwable {
                        if (channel.isOpen())
                          WebSockets.sendText("data:" + flowItem.value().toString(), channel, null);
                      }
                    });


                channel.getReceiveSetter().set(new AbstractReceiveListener() {

                  @Override
                  //On unsubscribe, unsubscribe the subscriber
                  protected void onClose(WebSocketChannel webSocketChannel,
                      StreamSourceFrameChannel channel) throws IOException {
                    super.onClose(webSocketChannel, channel);
                    //subscriptionFactory.unsubscribe(subscriberResult.getSubscriber());
                  }

                  @Override
                  //On error, unsubscribe the subscriber
                  protected void onError(WebSocketChannel channel, Throwable error) {
                    super.onError(channel, error);
                    //subscriptionFactory.unsubscribe(subscriberResult.getSubscriber());
                  }

                  @Override
                  //On Ping Send Events To Web Subscriber
                  protected void onFullTextMessage(WebSocketChannel channel,
                      BufferedTextMessage message) {
                    processMessageData(message, EventChannel.channel(eventStreamName).flow());
                  }
                });
                channel.resumeReceives();
              }

            })).addPrefixPath("/web", resource(new FileResourceManager(
            new File(WEEServer.class.getResource("/web/index.html").getFile())))
            .setDirectoryListingEnabled(true))).build();
    server.start();

    return this;
  }

  private void processMessageData(BufferedTextMessage message,
      ItemFlowable<Event, String> eventStream) {
    String messageData = message.getData();
    messageData = messageData.replaceAll(PING_CHAR_STRING, "");
    {
      if (messageData.isEmpty())
        ;
      else {
        String[] events = messageData.split("events:");
        for (String event : events) {
          try {
            if (!event.isEmpty()) {
              Event[] eventsToPut = JSONUtil.fromJSONString(event, Event[].class);
              Arrays.stream(eventsToPut).filter(s -> s.getEventNodeId() != EventNodeID.getNodeID())
                  .forEach(eventStream::putItem);
              if (!message.isComplete())
                logger.info("B");
            }
          } catch (IOException e) {
            logger.info(e.getMessage());
          }
        }
      }
    }
  }

}
