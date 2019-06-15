package org.xio.one.reactive.http.weeio;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.*;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.MultiItemSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;
import org.xio.one.reactive.http.weeio.internal.api.ChannelApiBootstrap;
import org.xio.one.reactive.http.weeio.internal.api.JSONUtil;
import org.xio.one.reactive.http.weeio.internal.domain.Event;
import org.xio.one.reactive.http.weeio.internal.domain.EventNodeID;
import org.xio.one.reactive.http.weeio.internal.domain.request.PassthroughExpression;
import org.xio.one.reactive.http.weeio.internal.domain.response.SubscriptionResponse;
import org.xio.one.reactive.http.weeio.internal.service.EventChannel;
import org.xnio.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Test Client for WeeioHTTPServer
 *
 * @Author Richard Durley
 * @OringinalWork XIO.ONE
 * @Copyright Richard Durley
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 */
public class EdgeServer {

  private static String PING_CHAR_STRING = Character.toString('�');
  private static Logger logger = Logger.getLogger(EdgeServer.class.getCanonicalName());

  public static void main(final String[] args) throws InterruptedException {

    Thread.sleep(10000);
    Random randomApiPort = new Random();

    try {
      String serverHostIPAddress;
      String wsHostIPAddress;
      List<String> argList = Arrays.asList(args);
      //EdgeServer eventServer = new EdgeServer();
      final String channelName;

      if (argList.contains("--h") || argList.contains("--help")) {
        System.out.println("Usage Help");
        System.out.println("[-n name] : connect to stream with name, default name is events");
        System.out.println("[-ip ipaddress] ipaddress for master server default 0.0.0.0");
        System.out.println("[-ws port] port for master server web socket server default 7000");
        System.out.println(
            "[-ap port] port for master server api port on which to registerCompletableFuture new subscription default 8080");
        System.out.println();
        System.out.println("Example java WEEiOTestClient -n events -ip 0.0.0.0 -ws 7000 -ap 8080");
        System.exit(0);
      }

      if (argList.contains("-n")) {
        channelName = argList.get((argList.indexOf("-n") + 1));
      } else {
        channelName = "events";
      }
      logger.info("**** configuring client connect to channel " + channelName);

      EventChannel.channel(channelName);
      EventChannel.channel(channelName + "_sink");

      if (argList.contains("-ip")) {
        serverHostIPAddress = argList.get((argList.indexOf("-ip") + 1));
      } else
        serverHostIPAddress = "0.0.0.0";
      logger.info("**** configuring client connect to edge ip " + serverHostIPAddress);

      if (argList.contains("-wsh")) {
        wsHostIPAddress = argList.get((argList.indexOf("-wsh") + 1));
      } else
        wsHostIPAddress = "0.0.0.0";
      logger.info("**** configuring client connect to socket host ip " + serverHostIPAddress);

      int serverPort;
      if (argList.contains("-wsp")) {
        serverPort = Integer.parseInt(argList.get((argList.indexOf("-ws") + 1)));
      } else
        serverPort = 7000;
      logger.info("**** configuring client server port " + serverPort);

      int apiPort;
      if (argList.contains("-ap")) {
        apiPort = Integer.parseInt(argList.get((argList.indexOf("-ap") + 1)));
      } else {
        int offset = Math.abs(randomApiPort.nextInt(1998));
        apiPort = 8001+offset;
      }
      logger.info("**** configuring client server api port " + apiPort);
      ChannelApiBootstrap.startChannelAPI(serverHostIPAddress, apiPort);
      logger.info(
          "**** starting sever api @ http://" + serverHostIPAddress + ":" + apiPort + "/channel/"
              + channelName);

      int subscriptionPort = 8000;
      final String remoteWSURL =
          "ws://" + wsHostIPAddress + ":" + serverPort + "/" + channelName + "/subscribe/"
              + subscribeToChannel(channelName, wsHostIPAddress, subscriptionPort);

      spawnNewClient(remoteWSURL, channelName);

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

  }

  private static void spawnNewClient(String remoteWSURL, String channelName) {
    new Thread(() -> {
      try {
        final Xnio xnio = Xnio.getInstance("nio", EdgeServer.class.getClassLoader());
        final XnioWorker worker = xnio.createWorker(
            OptionMap.builder().set(Options.WORKER_IO_THREADS, 1)
                .set(Options.CONNECTION_HIGH_WATER, 100).set(Options.CONNECTION_LOW_WATER, 1)
                .set(Options.WORKER_TASK_CORE_THREADS, 10).set(Options.WORKER_TASK_MAX_THREADS, 100)
                .set(Options.TCP_NODELAY, true).set(Options.CORK, true).getMap());

        boolean connected = false;
        while (!connected) {
          try {
            WebSocketChannel webSocketChannel =
                withWebSocketEventClient(remoteWSURL, worker, channelName);
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
  }

  private static String subscribeToChannel(String channelName, String wsHostIPAddress,
      int apiPort) {
    ResteasyClient client = new ResteasyClientBuilder().build();
    String uri = "http://" + wsHostIPAddress + ":" + apiPort + "/" + channelName + "/subscribe";
    logger.info("Subscribing to: " + uri);
    ResteasyWebTarget target = client.target(uri);
    Response response = target.request()
        .post(Entity.entity(new PassthroughExpression(), MediaType.APPLICATION_JSON_TYPE));
    SubscriptionResponse subscriptionResponse = response.readEntity(SubscriptionResponse.class);
    response.close();
    EventNodeID.setNodeID(subscriptionResponse.getClientID());
    return subscriptionResponse.getClientID();
  }


  private static WebSocketChannel withWebSocketEventClient(String remoteURL, XnioWorker worker,
      String channelName) throws Exception {
    IoFuture<WebSocketChannel> connection;
    Subscriber<String, Event> subscriber;

    connection = WebSocketClient
        .connectionBuilder(worker, new DefaultByteBufferPool(true, 65536), new URI(remoteURL))
        .setClientExtensions(Set.of(new PerMessageDeflateHandshake(true, 6))).connect();
    Thread.currentThread().sleep(1000);
    if (connection.getStatus() == IoFuture.Status.DONE) {

      logger.info(".");
      logger.info("Connected to cluster server@" + remoteURL);
      WebSocketChannel channel = connection.get();

      subscriber = EventChannel.channel("events").flow()
          .addSubscriber(new MultiItemSubscriber<String, Event>() {
            @Override
            public void onNext(Stream<? extends Item<Event>> items) {
              if (channel.isOpen())
                items.forEach(i -> WebSockets.sendText(i.value().toFullJSONString(), channel, null));
            }
          });


      channel.getReceiveSetter().set(new AbstractReceiveListener() {

        @Override
        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel)
            throws IOException {
          super.onClose(webSocketChannel, channel);
          logger.log(Level.WARNING, "close connection received from server");
          subscriber.stop();
          spawnNewClient(remoteURL, channelName);
        }

        @Override
        protected void onError(WebSocketChannel channel, Throwable error) {
          super.onError(channel, error);
          logger.log(Level.SEVERE, "error received from socket", error);
          subscriber.stop();
          spawnNewClient(remoteURL, channelName);
        }

        @Override
        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message)
            throws IOException {
          processMessageData(message, channelName);
        }
      });

      new Thread(() -> {
        while (channel.isOpen()) {
          try {
            WebSockets.sendTextBlocking(PING_CHAR_STRING, channel);
            Thread.sleep(1000);
          } catch (Exception e) {
            logger.log(Level.WARNING, "channel IO exception", e);
          }
          channel.resumeReceives();
        }
        logger.log(Level.SEVERE, "channel was closed");
      }).start();
      return channel;
    } else {
      connection.cancel();
    }
    return null;
  }

  private static void processMessageData(BufferedTextMessage message, String channelName) {
    String messageData = message.getData();
    ItemFlowable<Event, String> itemFlowable = EventChannel.channel(channelName + "_sink").flow();
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
              logger.fine(event);
              Event[] eventsToPut;
              if (event.startsWith("["))
                eventsToPut = JSONUtil.fromJSONString(event, Event[].class);
              else
                eventsToPut = (Event[]) List.of(JSONUtil.fromJSONString(event, Event.class))
                    .toArray(new Event[0]);
              Arrays.stream(eventsToPut).forEach(itemFlowable::putItem);
              if (!message.isComplete())
                logger.log(Level.SEVERE,"Incomplete message " + messageData);
            }
          } catch (IOException e) {
            logger.log(Level.SEVERE,"Error processing message ", e);
          }
        }
      }
    }
  }

}
