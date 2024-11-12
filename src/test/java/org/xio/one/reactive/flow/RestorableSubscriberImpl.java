package org.xio.one.reactive.flow;

import java.util.Map;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;

public class RestorableSubscriberImpl implements RestorableSubscriber<String,String> {

  @Override
  public Subscriber<String, String> restore(String id, Map<String, Object> context) {

    ItemSubscriber<String, String> restoredSubscriber = new ItemSubscriber<String, String>(id) {

      private String lastValue;

      @Override
      public void onNext(Item<String> item) {
        lastValue = item.getItemValue();
        System.out.println(lastValue);
      }

      @Override
      public String finalise() {
        return this.lastValue;
      }

      @Override
      public void restoreContext(Map<String, Object> context) {
        lastValue = (String) context.get("lastValue");
        if (lastValue.isEmpty()) {
          throw new RuntimeException();
        }
      }
    };
    restoredSubscriber.restoreContext(context);
    return restoredSubscriber;
  }

}
