package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;

public class RecoverySnapshot<R,T> {

    Item[] contents;
    Subscriber<R,T>[] subscribers;
    public RecoverySnapshot(Item[] contents, Subscriber<R, T>[] subscribers) {
        this.contents=contents;
        this.subscribers=subscribers;
    }

    public Item[] getContents() {
        return contents;
    }

    public Subscriber<R, T>[] getSubscribers() {
        return subscribers;
    }


}
