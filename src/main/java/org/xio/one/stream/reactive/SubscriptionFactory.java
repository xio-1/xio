package org.xio.one.stream.reactive;

import org.xio.one.stream.AsyncStream;

public class SubscriptionFactory<T> {

  public SubscriberResult<T> subscribe(AsyncStream eventStream, Subscriber<T> subscriber) {
    Subscription<?> subscription = new Subscription<>(eventStream,subscriber);
    SubscriptionExecutor.instance().addSubscription(subscription);
    return subscriber;
  }

  public void unsubscribe(Subscriber<T> subscriber) {
    SubscriptionExecutor.instance().removeSubscription(subscriber);
  }
}
