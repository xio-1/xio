package org.xio.one.reactive.flow.domain.flow;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class Promise<R> {

  private ConcurrentHashMap<String, SubscriberPromise<R>> promises;

  public Iterator<SubscriberPromise<R>> results() {
      return promises.values().iterator();
  }

  public class SubscriberPromise<R> {

    private final String subscriberID;
    private final Future<R> future;
    private String promiseId;

    public SubscriberPromise(String subscriberID, Future<R> future) {
      this.promiseId = UUID.randomUUID().toString();
      this.subscriberID = subscriberID;
      this.future = future;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      SubscriberPromise that = (SubscriberPromise) o;

      return promiseId.equals(that.promiseId);

    }

    @Override
    public int hashCode() {
      return promiseId.hashCode();
    }

    public String getSubscriberID() {
      return subscriberID;
    }

    public Future<R> getFuture() {
      return future;
    }

    public String getPromiseId() {
      return promiseId;
    }
  }

  public Promise() {
    this.promises = new ConcurrentHashMap<>();
  }

  public void addPromise(String subscriberID, Future<R> future) {
    SubscriberPromise<R> subscriberPromise = new SubscriberPromise<>(subscriberID, future);
    this.promises.put(subscriberPromise.getPromiseId(),subscriberPromise);
  }


}
