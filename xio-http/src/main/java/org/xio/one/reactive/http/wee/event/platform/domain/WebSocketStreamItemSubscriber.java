package org.xio.one.reactive.http.wee.event.platform.domain;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscriber.StreamItemSubscriber;

public class WebSocketStreamItemSubscriber extends StreamItemSubscriber<String, Event> {

  WebSocketChannel channel;

  public WebSocketStreamItemSubscriber(WebSocketChannel webSocketChannel) {
    this.channel =webSocketChannel;
  }
  @Override
  public void onNext(Item<Event, String> flowItem) throws Throwable {
    if (channel.isOpen())
      WebSockets.sendText("data: " + flowItem.value().toString(), channel,
          null);
  }


}
