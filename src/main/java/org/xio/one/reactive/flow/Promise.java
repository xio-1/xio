package org.xio.one.reactive.flow;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class Promise<R> {

  private final ConcurrentHashMap<String, SubscriberPromise<R>> promises;

  public Promise() {
    this.promises = new ConcurrentHashMap<>();
  }

  public List<Future<R>> results() {
    return promises.values().stream().map(SubscriberPromise::getFuture)
        .collect(Collectors.toList());
  }

  public Future<R> result(String subscriberId) {
    return promises.get(subscriberId).getFuture();
  }

  protected void addPromise(String subscriberID, Future<R> future) {
    SubscriberPromise<R> subscriberPromise = new SubscriberPromise<>(subscriberID, future);
    this.promises.put(subscriberPromise.subscriberID, subscriberPromise);
  }

  public class SubscriberPromise<R> {

    private final String subscriberID;
    private final Future<R> future;

    public SubscriberPromise(String subscriberID, Future<R> future) {
      this.subscriberID = subscriberID;
      this.future = future;
    }

    public String getSubscriberID() {
      return subscriberID;
    }

    public Future<R> getFuture() {
      return future;
    }

  }

}
