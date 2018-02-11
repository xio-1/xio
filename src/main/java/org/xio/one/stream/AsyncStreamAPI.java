package org.xio.one.stream;

import org.xio.one.stream.event.Event;
import org.xio.one.stream.selector.Selector;

import java.util.HashMap;

/**
 * StreamAPI - API that allows streams to be created, queried, and removed @Author Rich
 * Durley @Copyright Rich Durley 2014
 */
public class AsyncStreamAPI {

  private static HashMap<String, AsyncStream> streams = new HashMap<String, AsyncStream>();

  /**
   * Create a new streamContents with given name and event time to live
   *
   * @param streamName
   * @param ttl
   * @return
   */
  public static AsyncStream stream(String streamName, int ttl) {
    AsyncStream eventStream;
    eventStream = new AsyncStream(streamName, null, new Selector(), null, ttl);
    streams.put(streamName, eventStream);
    return eventStream;
  }

  /**
   * Create a new streamContents with given name, array of selector to work events, with selector
   * result ordered by the given order by value (0-timestamp, 1-eventID) optional time to live of
   * events can be specified when timestamp ordering is selected
   *
   * @return APIResource
   */
  public static AsyncStream stream(AsyncStreamDefinition eventStreamDefinition) {
    AsyncStream eventStream;
    eventStream =
        new AsyncStream(
            eventStreamDefinition.getName(),
            eventStreamDefinition.getIndexFieldName(),
            eventStreamDefinition.getSelector(),
            eventStreamDefinition.getWorkerParams(),
            eventStreamDefinition.getEventTTLSeconds());
    streams.put(eventStreamDefinition.getName(), eventStream);
    return eventStream;
  }

  public static AsyncStream stream(String streamName) {
    return stream(streamName, Object.class, Object.class);
  }

  /**
   * Get or create the streamContents given the streams name
   *
   * @param streamName
   * @return
   * @throws StreamNotFoundException
   */
  public static <E, R> AsyncStream<E, R> stream(
      String streamName, Class<E> event, Class<R> result) {
    if (streams.get(streamName) == null) {
      AsyncStream eventStream;
      eventStream = new AsyncStream<E, R>(streamName, null, new Selector(), null, -1);
      streams.put(streamName, eventStream);
      return (AsyncStream<E, R>) eventStream;
    } else return (AsyncStream<E, R>) streams.get(streamName);
  }

  /**
   * Executes the contents of type contents type on the streamContents with the given contents
   * parameters
   *
   * @param streamname
   * @param queryType
   * @param queryParams
   * @return
   */
  public static Object queryStream(String streamname, QueryType queryType, Object... queryParams)
      throws StreamNotFoundException {

    AsyncStream eventStream = getStream(streamname);

    switch (queryType) {
      case ALL:
        {
          return eventStream.contents().getAll();
        }
      case ALLBY:
        {
          // return eventStream.contents().getAllBy((String) queryParams[0], queryParams[1]);
        }
      case LAST:
        {
          return eventStream.contents().getLast();
        }
      case FIRST:
        {
          return eventStream.contents().getFirst();
        }
      case ALLAFTER:
        {
          return eventStream.contents().getAllAfter((Event) queryParams[0]);
        }
      case AVERAGE:
        {
          // return eventStream.contents().getAverage((String) queryParams[0]);
        }
      case LASTBY:
        {
          // return eventStream.contents().getLastSeenEventByFieldnameValue((String) queryParams[0],
          // queryParams[1]);
        }
      case COUNT:
        {
          return eventStream.contents().getCount();
        }
      case CLOSE:
        {
          eventStream.end(false);
        }
      default:
        {
          throw new UnsupportedOperationException(queryType.toString());
        }
    }
  }

  private static AsyncStream getStream(String streamName) throws StreamNotFoundException {
    AsyncStream eventStream = streams.get(streamName);
    if (eventStream == null) throw new StreamNotFoundException(streamName);
    else return eventStream;
  }

  public enum QueryType {
    ALL,
    LAST,
    FIRST,
    ALLAFTER,
    ALLBEFORE,
    AVERAGE,
    Max,
    Min,
    COUNT,
    CONTAINS,
    LASTBY,
    AverageBy,
    CountBy,
    MaxBy,
    MinBy,
    AllBeforeTimestamp,
    AllAfterTimestamp,
    Previous,
    PreviousBy,
    ALLBY,
    CLOSE
  }
}
