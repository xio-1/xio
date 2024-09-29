package org.xio.one.reactive.flow.subscribers;

import java.util.Map;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;

public abstract class RestoreSubscribers<R,T> {

  public void restoreAll(Map<String, Map<String, Object>> subscriberContext, ItemFlowable<T,R> itemFlowable) {
    subscriberContext.forEach((k,v)-> itemFlowable.addSubscriber(restoreEach(k, subscriberContext.get(k))));
  }

  abstract protected Subscriber<R,T> restoreEach(String k, Map<String, Object> stringObjectMap);

}
