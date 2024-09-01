package org.xio.one.reactive.flow.domain.flow;

import org.xio.one.reactive.flow.ItemSink;
import org.xio.one.reactive.flow.internal.RecoverySnapshot;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.FunctionalSubscriber;

public interface Flowable<T, R> {

  String name();

  String getUUID();

  Flowable<T, R> enableImmediateFlushing();

  Item<T>[] takeSinkSnapshot();

  RecoverySnapshot<R,T> takeRecoverySnapshot();

  ItemSink<T> getSink();

  void close(boolean waitForEnd);

  boolean hasEnded();

  int size();

  boolean isEmpty();

  long maxTTLSeconds();

  boolean housekeep();

  FunctionalSubscriber<R, T> publishTo(Class clazz);

  void resetLastSeenItem(String subscriberID, Item<T> lastSeenItem);

}
