package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.events.Event;
import org.xio.one.reactive.flow.subscribers.Subscriber;
import org.xio.one.reactive.flow.events.EmptyEvent;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

class Subscription<R, E> {

  private Event lastSeenEvent = null;
  private AsyncFlow<E,R> eventStream;
  private Future subscription;
  private Subscriber<R, E> subscriber;

  public Subscription(AsyncFlow<E, R> eventStream, Subscriber<R, E> subscriber) {
    this.eventStream = eventStream;
    this.subscriber = subscriber;
  }

  public AsyncFlow getEventStream() {
    return eventStream;
  }

  public Future<R> subscribe() {
    subscriber.initialise();
    CompletableFuture<R> completableFuture = new CompletableFuture<>();
    this.subscription =
        eventStream
            .executorService()
            .submit(
                () -> {
                  while ((!eventStream.hasEnded() || !eventStream.contents().hasEnded())
                      && !subscriber.isDone()) {
                    processResults(subscriber);
                  }
                  processFinalResults(subscriber);
                  unsubscribe();
                  completableFuture.complete(subscriber.getNext());
                });
    return completableFuture;
  }

  private void processFinalResults(Subscriber<R, E> subscriber) {
    NavigableSet<Event<E>> streamContents = streamContents();
    while (streamContents.size() > 0) {
      subscriber.emit(streamContents.stream());
      streamContents = streamContents();
    }
  }

  private void processResults(Subscriber<R, E> subscriber) {
    NavigableSet<Event<E>> streamContents = streamContents();
    if (streamContents.size() > 0) subscriber.emit(streamContents.stream());
  }

  public void unsubscribe() {
    this.subscriber.stop();
  }

  protected NavigableSet<Event<E>> streamContents() {
    NavigableSet<Event<E>> streamContents =
        Collections.unmodifiableNavigableSet(eventStream.contents().allAfter(lastSeenEvent));
    if (streamContents.size() > 0) lastSeenEvent = streamContents.last();
    return streamContents;
  }

  public Subscriber<R, E> getSubscriber() {
    return subscriber;
  }

  public Event getLastSeenEvent() {
    if (lastSeenEvent!=null)
      return lastSeenEvent;
    else
      return EmptyEvent.EMPTY_EVENT;
  }
}
