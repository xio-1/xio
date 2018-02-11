package org.xio.one.stream.reactive;

import org.xio.one.stream.selector.FilterEntry;

import java.net.URI;

public class Subscribers {
  public static CounterSubscriber Counter() {
    return new CounterSubscriber();
  }
  public static CollectorSubscriber CollectorSubscriber() {
    return new CollectorSubscriber();
  }

  public static SelectorCollectorSubscriber SelectorCollectorSubscriber(FilterEntry filterEntry) {
    return new SelectorCollectorSubscriber(filterEntry);
  }

  public static StreamHTTPForwarderSubscriber StreamHTTPForwarderSubscriber(URI uri,
      FilterEntry filterEntry) {
    return new StreamHTTPForwarderSubscriber(uri, filterEntry);
  }

  public static StreamHTTPForwarderSubscriber StreamHTTPForwarderSubscriber(URI uri) {
    return new StreamHTTPForwarderSubscriber(uri);
  }
}
