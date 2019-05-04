package org.xio.one.reactive.http.weeio;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;
import org.xio.one.reactive.http.weeio.internal.CORSResourceHeadersHandler;
import org.xio.one.reactive.http.weeio.internal.api.ApiBootstrap;
import org.xio.one.reactive.http.weeio.internal.api.JSONUtil;
import org.xio.one.reactive.http.weeio.internal.domain.Event;
import org.xio.one.reactive.http.weeio.internal.domain.EventNodeID;
import org.xio.one.reactive.http.weeio.internal.service.EventChannel;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

;

/**
 * Web Events EveryWhere Input / Output  (WeeIO) @ Copyright Richard Durley 2019
 * <p>
 * The idea here is the propagation of JSON web events across the web in
 * near real time using a master cluster <-> edge(s) <-> client(s) pattern
 * The rational here is to implement highly scalable distributed
 * two way messaging over HTTP(S) (using web sockets)
 * <p>
 * It uses XIO as the core streaming and subscriber implementation
 * using Undertow to provide async servlet container for api and web
 * socket connections.
 * <p>
 * WeeIO HTTP Event Server
 *
 * @Author Richard Durley
 * @OringinalWork XIO.ONE
 * @Copyright Richard Durley
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 */
public class SocketServer {

  private static String PING_CHAR_STRING = Character.toString('�');
  private static Logger logger = Logger.getLogger(SocketServer.class.getCanonicalName());
  UndertowJaxrsServer server;

  public static void main(final String[] args) throws IOException {

    try {
      XIOService.start();

      String serverHostIPAddress = "0.0.0.0";
      List<String> argList = Arrays.asList(args);
      SocketServer eventServer = new SocketServer();
      final String channelName;

      if (argList.contains("--h") || argList.contains("--help")) {
        System.out.println("Usage Help");
        System.out.println("[-n name] : create stream with name, default name is events");
        System.out.println(
            "[-maxTTLSeconds integer] : event time to live in seconds, default is 1 second");
        System.out.println("[-ws port] start server data channel (web socket) on given port");
        System.out
            .println("[-c ipaddress:wsport] cluster to another master server @ipaddress:port");
        System.out
            .println("[-k shared secret] shared secret to be passed in X-AUTHORIZATION header");
        System.out.println("[-ip ipAddress] bind server to given ip address");
        System.out.println("[-in interface name] bind server to given interface adapter name");
        System.out.println("[-p port] start stream web api on given port");
        System.out.println();
        System.out.println("Example java WEEiOServer -n events -maxTTLSeconds 1 -ws 7222 -a 8080");
        System.exit(0);
      }

      if (argList.contains("-n")) {
        channelName = argList.get((argList.indexOf("-n") + 1));
      } else {
        channelName = "events";
      }

      int ttl = 10;
      if (argList.contains("-maxTTLSeconds")) {
        ttl = Integer.parseInt(argList.get((argList.indexOf("-maxTTLSeconds") + 1)));
      }

      EventChannel.channel(channelName);

      logger.info("**** created channel" + channelName + " with maxTTLSeconds seconds " + ttl);

      if (argList.contains("-in")) {
        String interface_name = argList.get((argList.indexOf("-in") + 1));
        serverHostIPAddress = getIPAddress(interface_name);
        logger.info("**** configuring server host using interface " + interface_name + ":"
            + serverHostIPAddress);

      }

      if (argList.contains("-ip")) {
        if (!argList.contains("-in")) {
          serverHostIPAddress = argList.get((argList.indexOf("-ip") + 1));
          logger.info("**** configuring server host using provided ip " + serverHostIPAddress);
        }
      }

      int serverPort = 7000;
      if (argList.contains("-ws")) {
        serverPort = Integer.parseInt(argList.get((argList.indexOf("-ws") + 1)));
      }

      eventServer.withWebSocketEventServer(channelName, serverHostIPAddress, serverPort, ttl);

      logger.info(
          "**** started master server socket @ http://" + serverHostIPAddress + ":" + serverPort
              + "/" + channelName + "/subscribe");


      /*if (argList.contains("-c")) {
        final String clientURL =
            "http://" + argList.get(argList.indexOf("-c") + 1) + "/" + channelName + "/publishTo";

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
      if (argList.contains("-p")) {
        apiport = Integer.parseInt(argList.get((argList.indexOf("-a") + 1)));
      }

      ApiBootstrap.start(serverHostIPAddress, apiport);
      logger.info(
          "**** starting sever api @ http://" + serverHostIPAddress + ":" + apiport + "/channel/"
              + channelName);


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

  /*public WebSocketChannel withWebSocketEventClient(String remoteURL, String eventStreamName,
      XnioWorker worker) throws Exception {


    EventChannel.channel(eventStreamName).flow()
        .addSubscriber(new StreamItemSubscriber<String, Event>() {
                         @Override
                         public void onError(FlowItem<Event, String> flowItem) throws Throwable {

                         }
                       }

        );
    SubscriberResult<Event[]> subscriberResult =
        (new Subscription<>(eventStream, Subscribers.CollectorSubscriber())).publishTo();
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
          eventStream.close(true);
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

  public SocketServer withWebSocketEventServer(String eventStreamName, String serverHostIPAddress,
      final int port, int ttl) throws IOException {

    final Xnio xnio = Xnio.getInstance("nio", SocketServer.class.getClassLoader());
    final XnioWorker worker = xnio.createWorker(
        OptionMap.builder().set(Options.WORKER_IO_THREADS, 8)
            .set(Options.CONNECTION_HIGH_WATER, 10000).set(Options.CONNECTION_LOW_WATER, 10)
            .set(Options.WORKER_TASK_CORE_THREADS, 30).set(Options.WORKER_TASK_MAX_THREADS, 30)
            .set(Options.TCP_NODELAY, true).set(Options.CORK, true).getMap());

    Undertow server =
        Undertow.builder().addHttpListener(port, serverHostIPAddress).setWorker(worker).setHandler(
            path().addPrefixPath("/" + eventStreamName + "/subscribe",
                websocket(new WebSocketConnectionCallback() {

                  ItemSubscriber<String, Event> streamItemSubscriber;

                  @Override
                  public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                    String path = URI.create(exchange.getRequestURI()).getPath();
                    String clientID = path.substring(path.lastIndexOf("/") + 1);
                    try {
                      if (clientID.isBlank() || clientID.isEmpty()) {
                        throw new SecurityException("No client credentials provided");
                      }
                      String subscriberId = clientID;
                      logger.info("A subscribers with clientID " + subscriberId
                          + " is trying to connect too WEEIOServer");

                      streamItemSubscriber =
                          EventChannel.channel(eventStreamName).getSubscriber(subscriberId);


                      if (streamItemSubscriber == null)
                        streamItemSubscriber = EventChannel.channel(eventStreamName)
                            .newSubscriber(channel, subscriberId);

                      logger.info("Subscriber " + streamItemSubscriber.getId() + " for client "
                          + subscriberId + " " + "has " + "connected");
                      channel.getReceiveSetter().set(new AbstractReceiveListener() {

                        @Override
                        //On unsubscribe, unsubscribe the subscribers
                        protected void onClose(WebSocketChannel webSocketChannel,
                            StreamSourceFrameChannel channel) {
                          logger.info("Subscriber " + streamItemSubscriber.getId()
                              + " closed connection to WEEIOServer");
                          EventChannel.channel(eventStreamName).removeSubscriber(subscriberId);
                          channel.finalFrame();
                          try {
                            webSocketChannel.close();
                          } catch (IOException e) {
                            logger.log(Level.SEVERE, e.getMessage(), e);
                          }
                        }

                        @Override
                        protected void onError(WebSocketChannel channel, Throwable error) {
                          logger.log(Level.WARNING, "Subscriber " + streamItemSubscriber.getId()
                              + " sent error to WEEIOServer ", error);
                          try {
                            channel.sendClose();
                          } catch (IOException e) {
                            logger.log(Level.WARNING, e.getMessage(), e);
                          }
                        }

                        @Override
                        //On Ping Send Events To Web Subscriber
                        protected void onFullTextMessage(WebSocketChannel channel,
                            BufferedTextMessage message) {
                          processMessageData(message, EventChannel.channel(eventStreamName).flow());
                        }


                      });
                      channel.resumeReceives();
                    } catch (Exception e) {
                      logger.log(Level.WARNING, "A valid clientID was not provided");
                      try {
                        channel.setCloseReason("Invalid clientID");
                        channel.sendClose();
                        channel.close();
                      } catch (IOException e2) {
                      }
                    }
                  }

                }).addExtension(new PerMessageDeflateHandshake(false, 6))).addPrefixPath("/web",
                new CORSResourceHeadersHandler(new FileResourceManager(
                    createWelcomeFile()))))
            .build();
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
        String[] events = messageData.split("data: ");

        for (int j = 0; j < events.length; j++) {
          String event = events[j];
          try {
            if (!event.isEmpty()) {
              Event[] eventsToPut;
              if (event.startsWith("["))
                eventsToPut = JSONUtil.fromJSONString(event, Event[].class);
              else
                eventsToPut = (Event[]) List.of(JSONUtil.fromJSONString(event, Event.class))
                    .toArray(new Event[0]);
              Arrays.stream(eventsToPut).filter(
                  s -> s.get_eventType() == null || s.get_eventNodeId() != EventNodeID.getNodeID())
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

  public File createWelcomeFile() throws IOException {
    String seedFile;
    try {
      logger.info("creating resource located at URI ");
      final BufferedReader bufferedReader = new BufferedReader(
          new InputStreamReader(SocketServer.class.getResourceAsStream("/web/index.html")));
      seedFile = bufferedReader.lines().collect(Collectors.joining("\n"));
      File index = new File("index.html");
      try (PrintWriter out = new PrintWriter(index)) {
        out.write(seedFile);
      } finally {
        bufferedReader.close();
      }
      return index;
    } catch (IOException e) {
      throw new IOException("Could not create index file",e);
    }

  }

}
