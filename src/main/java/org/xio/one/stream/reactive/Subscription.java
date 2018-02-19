package org.xio.one.stream.reactive;

import org.xio.one.stream.AsyncStream;
import org.xio.one.stream.event.Event;
import org.xio.one.stream.reactive.subscribers.Subscriber;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Subscription<R,E> {

  private Event lastSeenEvent = null;
  private AsyncStream eventStream;
  private Future subscription;
  private Subscriber<R,E> subscriber;
  private boolean alive = true;

  public Subscription(AsyncStream eventStream, Subscriber<R,E> subscriber) {
    this.eventStream = eventStream;
    this.subscriber = subscriber;
  }

  public AsyncStream getEventStream() {
    return eventStream;
  }

  public Future<R> subscribe() {
      subscriber.initialise();
      CompletableFuture<R> completableFuture = new CompletableFuture<>();
      this.subscription = eventStream.getExecutorService().submit(() -> {
        while ((!eventStream.hasEnded() || !eventStream.contents().hasEnded()) &&!subscriber.isDone()) {
          processResults(subscriber);
        }
        processResults(subscriber);
        completableFuture.complete(subscriber.getNext());
      });
      return completableFuture;
  }

  public void processResults() {
    processResults(subscriber);
  }

  private void processResults(Subscriber<R,E> subscriber) {
    NavigableSet<Event<E>> streamContents = streamContents();
    if (streamContents.size() > 0)
      subscriber.emit(streamContents.stream());
  }

  public void unsubscribe() {
    this.subscriber.stop();
  }

  protected NavigableSet<Event<E>> streamContents() {
    NavigableSet<Event<E>> streamContents =
        Collections.unmodifiableNavigableSet(eventStream.contents().getAllAfter(lastSeenEvent));
    if (streamContents.size() > 0)
      lastSeenEvent = streamContents.last();
    return streamContents;
  }

  public Subscriber<R,E> getSubscriber() {
    return subscriber;
  }
}
