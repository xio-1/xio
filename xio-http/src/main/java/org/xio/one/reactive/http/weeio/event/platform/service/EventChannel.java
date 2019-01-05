package org.xio.one.reactive.http.weeio.event.platform.service;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.ItemFlow;
import org.xio.one.reactive.http.weeio.event.platform.domain.Event;
import org.xio.one.reactive.http.weeio.event.platform.domain.WebSocketStreamItemSubscriber;
import org.xio.one.reactive.http.weeio.event.platform.domain.request.FilterExpression;
import org.xio.one.reactive.http.weeio.event.platform.domain.selector.FilterEntry;
import org.xio.one.reactive.http.weeio.event.platform.domain.selector.FilterSelector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventChannel {
  private static Map<String, EventChannel> channels = new HashMap<>();
  private ItemFlow<Event, String> flow;

  private EventChannel(ItemFlow<Event, String> anItemFlow) {
    this.flow=anItemFlow;
  }

  public static EventChannel channel(String name) {
    if (!channels.containsKey(name))
      channels.put(name, new EventChannel(Flow.anItemFlow(name,60)));
    return channels.get(name);
  }

  public ItemFlow<Event, String> flow() {
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

  public enum QueryType {
    STATUS, ALL, LAST, FIRST, ALLAFTER, ALLBEFORE, AVERAGE, Max, Min, COUNT, CONTAINS, LASTBY, AverageBy, CountBy, MaxBy, MinBy, AllBeforeTimestamp, AllAfterTimestamp, Previous, PreviousBy, ALLBY, CLOSE
  }


  private HashMap<String, WebSocketStreamItemSubscriber> clients = new HashMap<>();


  public String registerNewClient(FilterExpression filterExpression) {
    String clientID = UUID.randomUUID().toString();
    FilterSelector filterSelector = new FilterSelector();
    FilterEntry filterEntry = new FilterEntry(filterExpression.getField(),filterExpression.getOperator(),filterExpression.getValue());
    clients.put(clientID, null);
    return clientID;
  }

  public WebSocketStreamItemSubscriber getSubscriber(String subscriberId) {
    if (clients.containsKey(subscriberId)) {
      WebSocketStreamItemSubscriber webSocketStreamItemSubscriber = clients.get(subscriberId);
      if (webSocketStreamItemSubscriber != null)
        return (WebSocketStreamItemSubscriber) flow.getSubscriber(subscriberId);
      else
        return null;
    }
    throw new SecurityException();
  }

}
