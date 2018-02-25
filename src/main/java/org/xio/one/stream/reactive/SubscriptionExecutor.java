package org.xio.one.stream.reactive;

import org.xio.one.stream.event.Event;
import org.xio.one.stream.reactive.subscribers.Subscriber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

class SubscriptionExecutor<E> {

  private static SubscriptionExecutor instance;
  private static List<Subscription> subscriptionList =
      Collections.synchronizedList(new ArrayList<>());
  long count = 0;
  private AtomicLong counter = new AtomicLong(0);
  private Event lastSeenEvent = null;
  private AsyncStream eventStream;
  private Thread subscriptionThread;

  public static synchronized SubscriptionExecutor instance() {
    if (instance == null) {
      instance = new SubscriptionExecutor();
      instance.startSubscriberLoop();
    }
    return instance;
  }

  public void addSubscription(Subscription<?,E> subscription) {
    subscriptionList.add(subscription);
  }

  private void startSubscriberLoop() {
    new Thread() {
      @Override
      public void run() {
        while (true) {
          synchronized (subscriptionList) {
            subscriptionList.parallelStream().forEach(Subscription::processResults);
          }
        }
      }
    }.start();
  }

  public void removeSubscription(Subscriber subscriber) {
    synchronized (subscriptionList) {
      subscriptionList.remove(subscriber);
    }
  }
}
