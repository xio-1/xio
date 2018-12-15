package org.xio.one.reactive.http.wee.event.platform.service;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.ItemFlowable;
import org.xio.one.reactive.http.wee.event.platform.domain.Event;

import java.util.HashMap;
import java.util.Map;

public class EventChannel {
  private static Map<String, EventChannel> channels = new HashMap<>();
  private ItemFlowable<Event, String> flow;

  /**
   * Create a new streamContents with given name and platform time to live
   *
   * @param name
   * @return
   */
  public static EventChannel channel(String name) {
    if (channels.containsKey(name))
      return channels.get(name);
    else {
      return new EventChannel(name);
    }
  }

  private EventChannel(String name) {
    this.flow = Flow.anItemFlow(name);
    channels.put(name, this);
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

  public enum QueryType {
    STATUS, ALL, LAST, FIRST, ALLAFTER, ALLBEFORE, AVERAGE, Max, Min, COUNT, CONTAINS, LASTBY, AverageBy, CountBy, MaxBy, MinBy, AllBeforeTimestamp, AllAfterTimestamp, Previous, PreviousBy, ALLBY, CLOSE
  }


}
