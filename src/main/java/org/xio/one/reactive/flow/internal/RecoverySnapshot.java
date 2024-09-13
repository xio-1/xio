package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.FutureSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

public class RecoverySnapshot<R,T> {

    private final Map<String, Item<T>> lastSeenItemMap;
    private final List<FutureSubscriber<R, T>> futureSubscribers;
    private NavigableSet<Item<T>> contents;
    private List<Subscriber<R,T>> subscribers;
    long itemID;

    public RecoverySnapshot(long current, NavigableSet<Item<T>> contents, List<Subscriber<R, T>> subscribers, List<FutureSubscriber<R, T>> futureSubscribers, Map<String, Item<T>> lastSeenItemMap) {
        this.contents=contents;
        this.subscribers=subscribers;
        this.futureSubscribers=futureSubscribers;
        this.itemID=current;
        this.lastSeenItemMap = lastSeenItemMap;
    }

    public  NavigableSet<Item<T>> getContents() {
        return contents;
    }

    public long getItemID() {
        return itemID;
    }

    public List<Subscriber<R, T>> getSubscribers() {
        return subscribers;
    }

    public List<FutureSubscriber<R, T>> getFutureSubscribers() {
        return futureSubscribers;
    }

    public Map<String, Item<T>> getLastSeenItemMap() {
        return lastSeenItemMap;
    }
}
