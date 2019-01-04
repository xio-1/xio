package org.xio.one.reactive.http.wee;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.*;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.xio.one.reactive.http.wee.event.platform.api.JSONUtil;
import org.xio.one.reactive.http.wee.event.platform.domain.Event;
import org.xio.one.reactive.http.wee.event.platform.domain.response.SubscriptionResponse;
import org.xio.one.reactive.http.wee.event.platform.domain.request.PassthroughExpression;
import org.xio.one.reactive.http.wee.event.platform.service.EventChannel;
import org.xnio.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Richard Durley
 */
public class WEEiOTestClient {

  private static String PING_CHAR_STRING = Character.toString('�');
  private static Logger logger = Logger.getLogger(WEEiOTestClient.class.getCanonicalName());

  public static void main(final String[] args) {

    try {
      String serverHostIPAddress;
      List<String> argList = Arrays.asList(args);
      WEEiOTestClient eventServer = new WEEiOTestClient();
      final String channelName;

      if (argList.contains("--h") || argList.contains("--help")) {
        System.out.println("Usage Help");
        System.out.println("[-n name] : connect to stream with name, default name is events");
        System.out.println("[-ip ipaddress] ipaddress for master server default 0.0.0.0");
        System.out.println("[-ws port] port for master server web socket server default 7222");
        System.out.println(
            "[-ap port] port for master server api port on which to register new subscription default 8080");
        System.out.println();
        System.out.println("Example java WEEiOTestClient -n events -ip 0.0.0.0 -ws 7222 -ap 8080");
        System.exit(0);
      }

      if (argList.contains("-n")) {
        channelName = argList.get((argList.indexOf("-n") + 1));
      } else {
        channelName = "events";
      }
      logger.info("**** configuring client connect to channel " + channelName);

      EventChannel.channel(channelName);

      if (argList.contains("-ip")) {
        serverHostIPAddress = argList.get((argList.indexOf("-ip") + 1));
      } else
        serverHostIPAddress = "0.0.0.0";
      logger.info("**** configuring client connect to server host ip " + serverHostIPAddress);

      int serverPort;
      if (argList.contains("-ws")) {
        serverPort = Integer.parseInt(argList.get((argList.indexOf("-ws") + 1)));
      } else
        serverPort = 7222;
      logger.info("**** configuring client server port " + serverPort);

      int apiPort;
      if (argList.contains("-ap")) {
        apiPort = Integer.parseInt(argList.get((argList.indexOf("-ap") + 1)));
      } else
        apiPort = 8080;
      logger.info("**** configuring client server api port " + serverPort);

      final String remoteWSURL =
          "ws://" + serverHostIPAddress + ":" + serverPort + "/channel/" + channelName
              + "/subscribe/" + subscribeToChannel(channelName,serverHostIPAddress,apiPort);

      new Thread(() -> {
        try {
          final Xnio xnio = Xnio.getInstance("nio", WEEiOTestClient.class.getClassLoader());
          final XnioWorker worker = xnio.createWorker(
              OptionMap.builder().set(Options.WORKER_IO_THREADS, 1)
                  .set(Options.CONNECTION_HIGH_WATER, 10).set(Options.CONNECTION_LOW_WATER, 1)
                  .set(Options.WORKER_TASK_CORE_THREADS, 1).set(Options.WORKER_TASK_MAX_THREADS, 1)
                  .set(Options.TCP_NODELAY, true).set(Options.CORK, true).getMap());

          boolean connected = false;
          while (!connected) {
            try {
              WebSocketChannel webSocketChannel =
                  eventServer.withWebSocketEventClient(remoteWSURL, channelName, worker);
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
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

  }

  private static String subscribeToChannel(String channelName, String serverHostIPAddress,
      int apiPort) {
    ResteasyClient client = new ResteasyClientBuilder().build();
    ResteasyWebTarget target = client.target("http://"+serverHostIPAddress+":"+apiPort+"/"+channelName+"/subscribe");
    Response response = target.request().post(Entity.entity(new PassthroughExpression(), MediaType.APPLICATION_JSON_TYPE));
    SubscriptionResponse subscriptionResponse = response.readEntity(SubscriptionResponse.class);
    response.close();
    return subscriptionResponse.getClientID();
  }


  public WebSocketChannel withWebSocketEventClient(String remoteURL, String eventStreamName,
      XnioWorker worker) throws Exception {
    IoFuture<WebSocketChannel> connection;

    connection = WebSocketClient
        .connectionBuilder(worker, new DefaultByteBufferPool(true, 32768), new URI(remoteURL))
        .connect();
    Thread.currentThread().sleep(1000);
    if (connection.getStatus() == IoFuture.Status.DONE) {
      logger.info(".");
      logger.info("Connected to cluster server@" + remoteURL);
      WebSocketChannel channel = connection.get();
      channel.getReceiveSetter().set(new AbstractReceiveListener() {

        @Override
        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel)
            throws IOException {
          super.onClose(webSocketChannel, channel);
          logger.log(Level.WARNING, "close connection received from server");
          System.exit(999);
        }

        @Override
        protected void onError(WebSocketChannel channel, Throwable error) {
          super.onError(channel, error);
          logger.log(Level.SEVERE, "error received from socket", error);
          System.exit(999);
        }

        @Override
        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message)
            throws IOException {
          processMessageData(message);
        }
      });

      new Thread(() -> {
        while (channel.isOpen()) {
          try {
            WebSockets.sendTextBlocking(PING_CHAR_STRING, channel);
          } catch (java.io.IOException e) {
            logger.log(Level.WARNING, "channel IO exception", e);
          }
          channel.resumeReceives();
        }
      }).start();
      return channel;
    } else {
      connection.cancel();
    }
    return null;

  }

  private void processMessageData(BufferedTextMessage message) {
    String messageData = message.getData();
    messageData = messageData.replaceAll(PING_CHAR_STRING, "");
    {
      if (messageData.isEmpty())
        ;
      else {
        String[] events = messageData.split("data: ");
        for (String event : events) {
          try {
            if (!event.isEmpty()) {
              Event[] eventsToPut;
              if (event.startsWith("["))
                eventsToPut = JSONUtil.fromJSONString(event, Event[].class);
              else
                eventsToPut = (Event[]) List.of(JSONUtil.fromJSONString(event, Event.class))
                    .toArray(new Event[0]);
              Arrays.stream(eventsToPut).forEach(e -> System.out.println("data:" + e.toString()));
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
