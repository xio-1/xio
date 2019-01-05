package org.xio.one.reactive.flow.subscribers.internal;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.NavigableSet;

/**
 * SubscriberInterface
 *
 * @Author Richard Durley
 * @OringinalWork XIO.ONE
 * @Copyright Richard Durley
 * @Licence @https://github.com/xio-1/xio/blob/master/LICENSE
 * @LicenceType Non-Profit Open Software License 3.0 (NPOSL-3.0)
 * @LicenceReference @https://opensource.org/licenses/NPOSL-3.0
 *
 */
public interface SubscriberInterface<R, T> {

  void emit(NavigableSet<Item<T, R>> e);

  boolean stop();

  boolean isDone();

  String getId();

  int delayMS();

  R getNext();

  void initialise();

  R getResult();

  void setResult(R result);

  void finalise();

  void process(NavigableSet<Item<T, R>> e);
}
