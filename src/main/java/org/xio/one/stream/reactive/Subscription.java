package org.xio.one.stream.reactive;

import org.xio.one.stream.AsyncStream;
import org.xio.one.stream.event.Event;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Subscription<E> {

  private Event lastSeenEvent = null;
  private AsyncStream eventStream;
  private Future subscription;
  private Subscriber<E> subscriber;
  private boolean alive = true;

  public Subscription(AsyncStream eventStream, Subscriber<E> subscriber) {
    this.eventStream = eventStream;
    this.subscriber = subscriber;
  }

  public AsyncStream getEventStream() {
    return eventStream;
  }

  public Future<E> subscribe() {
    if (subscription == null) {
      CompletableFuture<E> completableFuture = new CompletableFuture<>();
      this.subscription = AsyncStreamExecutor.cachedThreadPoolInstance().submit(() -> {
        while (!eventStream.hasEnded() || !eventStream.contents().hasEnded()) {
          processResults(subscriber);
        }
        processResults(subscriber);
        completableFuture.complete(subscriber.getNext());
      });
      return completableFuture;
    } else
      return null;
  }

  public void processResults() {
    processResults(subscriber);
  }

  private void processResults(Subscriber<E> subscriber) {
    NavigableSet<Event> streamContents = streamContents();
    if (streamContents.size() > 0)
      subscriber.emit(streamContents.stream());
  }

  public void unsubscribe() {
    this.subscriber.stop(true);
  }

  protected NavigableSet<Event> streamContents() {
    NavigableSet<Event> streamContents =
        Collections.unmodifiableNavigableSet(eventStream.contents().getAllAfter(lastSeenEvent));
    if (streamContents.size() > 0)
      lastSeenEvent = streamContents.last();
    return streamContents;
  }

  public Subscriber<E> getSubscriber() {
    return subscriber;
  }
}
