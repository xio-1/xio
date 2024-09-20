package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.FutureSubscriber;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

public class RecoverySnapshot<R,T> {

    private final Map<String, Item<T>> lastSeenItemMap;
    private final Map<String, Map<String, Object>> subscriberContext;
    private NavigableSet<Item<T>> contents;
    long itemID;

    public RecoverySnapshot(long current, NavigableSet<Item<T>> contents, Map<String, Item<T>> lastSeenItemMap, Map<String, Map<String, Object>> subscriberContext) {
        this.contents=contents;
        this.itemID=current;
        this.lastSeenItemMap = lastSeenItemMap;
        this.subscriberContext = subscriberContext;
    }

    public  NavigableSet<Item<T>> getContents() {
        return contents;
    }

    public long getItemID() {
        return itemID;
    }

    public Map<String, Item<T>> getLastSeenItemMap() {
        return lastSeenItemMap;
    }

    public Map<String, Map<String, Object>> getSubscriberContext() {
        return subscriberContext;
    }
}
