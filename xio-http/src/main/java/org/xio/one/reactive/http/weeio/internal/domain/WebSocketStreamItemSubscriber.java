package org.xio.one.reactive.http.weeio.internal.domain;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;

public class WebSocketStreamItemSubscriber extends ItemSubscriber<String, Event> {

  private final String subscriberId;
  WebSocketChannel channel;

  public WebSocketStreamItemSubscriber(WebSocketChannel channel, String subscriberId) {
    this.channel = channel;
    this.subscriberId = subscriberId;
  }

  @Override
  public void onNext(Item<Event, String> item)  {
    if (channel.isOpen() && !item.value().get_originId().equals(subscriberId))
      WebSockets.sendText("data: " + item.value().toJSONString(), channel, null);
  }


}
