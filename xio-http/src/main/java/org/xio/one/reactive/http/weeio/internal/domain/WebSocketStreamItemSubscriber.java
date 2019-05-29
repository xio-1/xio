package org.xio.one.reactive.http.weeio.internal.domain;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.MultiItemSubscriber;
import org.xio.one.reactive.http.weeio.internal.api.JSONUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebSocketStreamItemSubscriber extends MultiItemSubscriber<String, Event> {

  private final String subscriberId;
  WebSocketChannel channel;

  public WebSocketStreamItemSubscriber(WebSocketChannel channel, String subscriberId) {
    this.channel = channel;
    this.subscriberId = subscriberId;
  }

  @Override
  public void onNext(Stream<Item<Event, String>> items) {
    if (channel.isOpen()) {
      List<Event> toSend;
      toSend =
          items.filter(i -> !i.value().get_originId().equals(subscriberId)).map(Item::value)
          .collect(Collectors.toList());
      WebSockets.sendText("data: " + JSONUtil.toJSONString(toSend), channel, null);
    }
  }
}
