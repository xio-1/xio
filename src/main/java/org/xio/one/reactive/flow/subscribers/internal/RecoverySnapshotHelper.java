package org.xio.one.reactive.flow.subscribers.internal;

import org.xio.one.reactive.flow.domain.item.Item;

import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableSet;

public interface RecoverySnapshotHelper<R,T> {
    NavigableSet<Item<T>> copyContents(NavigableSet<Item<T>> steamContents);
    ArrayList<Subscriber<R, T>> copySubscribers(ArrayList<Subscriber<R, T>> subscribers);
    Map<String, Item<T>> copyLastSeenItemMap(Map<String, Item<T>> lastSeenItemMap);
}
