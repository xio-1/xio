package org.xio.one.reactive.http.weeio.internal.service;

import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.websockets.core.WebSocketChannel;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;
import org.xio.one.reactive.flow.subscribers.MultiItemSubscriber;
import org.xio.one.reactive.http.weeio.internal.api.ChannelApiBootstrap;
import org.xio.one.reactive.http.weeio.internal.domain.Event;
import org.xio.one.reactive.http.weeio.internal.domain.WebSocketStreamItemSubscriber;
import org.xio.one.reactive.http.weeio.internal.domain.request.FilterExpression;
import org.xio.one.reactive.http.weeio.internal.domain.selector.FilterEntry;
import org.xio.one.reactive.http.weeio.internal.domain.selector.FilterSelector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventChannel {
  private static Map<String, EventChannel> channels = new HashMap<>();
  private ItemFlowable<Event, String> flow;
  private HashMap<String, WebSocketStreamItemSubscriber> clients = new HashMap<>();

  private EventChannel(ItemFlowable<Event, String> anItemFlow) {
    this.flow = anItemFlow;
  }

  public static EventChannel channel(String name) {
    if (!channels.containsKey(name))
      channels.put(name, new EventChannel(Flow.anItemFlow(name, 60)));
    return channels.get(name);
  }

  public ItemFlowable<Event, String> flow() {
    return flow;
  }

  /**
   * Executes the query of type query type on the streamContents with the given query parameters
   *
   * @param streamname
   * @param queryType
   * @param queryParams
   * @return
   */
  public Object query(String streamname, QueryType queryType, Object... queryParams) {

    EventChannel channel = channels.get(streamname);

    switch (queryType) {

      case STATUS: {
        return !channel.flow.hasEnded();
      }

      case ALL: {
        return channel.flow().contents().allValues();
      }
      case LAST: {
        return channel.flow().contents().last();
      }
      case FIRST: {
        return channel.flow().contents().first();
      }
      default: {
        throw new UnsupportedOperationException(queryType.toString());
      }
    }
  }

  public boolean isAlive() {
    return !this.flow.hasEnded();
  }

  public String registerNewClient(FilterExpression filterExpression) {
    String clientID = UUID.randomUUID().toString();
    FilterSelector filterSelector = new FilterSelector();
    FilterEntry filterEntry =
        new FilterEntry(filterExpression.getField(), filterExpression.getOperator(),
            filterExpression.getValue());
    clients.put(clientID, null);
    return clientID;
  }

  public WebSocketStreamItemSubscriber getWebSocketSubscriber(String subscriberId) {
    if (clients.containsKey(subscriberId)) {
      WebSocketStreamItemSubscriber webSocketStreamItemSubscriber = clients.get(subscriberId);
      if (webSocketStreamItemSubscriber != null)
        return webSocketStreamItemSubscriber;
      else
        return null;
    } else
      throw new SecurityException("Unauthorized");
  }

  public MultiItemSubscriber<String, Event> newWebSocketSubscriber(WebSocketChannel channel,
      String subscriberId) {
    WebSocketStreamItemSubscriber webSocketStreamItemSubscriber =
        new WebSocketStreamItemSubscriber(channel, subscriberId);
    clients.replace(subscriberId, webSocketStreamItemSubscriber);
    return (MultiItemSubscriber<String, Event>) flow().addSubscriber(webSocketStreamItemSubscriber);
  }

  public void startSSEChannelClientSubscriber(String channelName) {
    channels.get(channelName).flow().addSubscriber(new ItemSubscriber<String, Event>() {
      @Override
      public void onNext(Item<Event> item) {
        for(ServerSentEventConnection h : ChannelApiBootstrap.getSseHandler().getConnections()) {
          h.send(item.value().toSSECompact());
        }

      }
    });
  }

  public void removeWebSocketSubscriber(String subscriberId) {
    WebSocketStreamItemSubscriber webSocketStreamItemSubscriber = clients.remove(subscriberId);
    flow().removeSubscriber(webSocketStreamItemSubscriber);
  }

  public enum QueryType {
    STATUS, ALL, LAST, FIRST, ALLAFTER, ALLBEFORE, AVERAGE, Max, Min, COUNT, CONTAINS, LASTBY, AverageBy, CountBy, MaxBy, MinBy, AllBeforeTimestamp, AllAfterTimestamp, Previous, PreviousBy, ALLBY, CLOSE
  }

}
