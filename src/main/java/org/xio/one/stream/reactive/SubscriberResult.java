package org.xio.one.stream.reactive;

import java.util.concurrent.TimeUnit;

/**
 * Created by Rich on 28/01/2017.
 */
public interface SubscriberResult<E> {
  E peek();

  E getNextAndReset();

  E getNextAndReset(long timeout, TimeUnit timeUnit);

  E getNext(long timeout, TimeUnit timeUnit);

  E getNext();

  Subscriber<E> getSubscriber();
}
