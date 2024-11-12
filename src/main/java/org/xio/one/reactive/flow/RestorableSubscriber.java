package org.xio.one.reactive.flow;

import org.xio.one.reactive.flow.subscribers.internal.Subscriber;

import java.util.Map;

public interface RestorableSubscriber<R, T> {
  Subscriber<R,T> restore(String id, Map<String, Object> context);
}
