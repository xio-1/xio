package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.FutureSubscriber;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

public class RecoverySnapshot<R,T> {

    private final Map<String, Item<T>> lastSeenItemMap;
    private NavigableSet<Item<T>> contents;
    long itemID;

    public RecoverySnapshot(long current, NavigableSet<Item<T>> contents, Map<String, Item<T>> lastSeenItemMap) {
        this.contents=contents;
        this.itemID=current;
        this.lastSeenItemMap = lastSeenItemMap;
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
}
