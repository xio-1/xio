package org.xio.one.reactive.flow.subscribers.internal;

import org.xio.one.reactive.flow.domain.item.Item;

import java.io.Serializable;
import java.util.NavigableSet;
import java.util.concurrent.Future;

/**
 * SubscriberInterface
 *
 * @Author Richard Durley
 * @OringinalWork XIO
 * @Copyright Richard Durley / XIO.ONE
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 */
public interface Subscriber<R, T> {

  void emit(NavigableSet<Item<T>> e);

  boolean stop();

  boolean isDone();

  String getId();

  int delayMS();

  R getNext();

  void initialise();

  Future<R> getFutureResult();

  void exitAndReturn(R result);

  R finalise();

  void process(NavigableSet<? extends Item<T>> e);


}
