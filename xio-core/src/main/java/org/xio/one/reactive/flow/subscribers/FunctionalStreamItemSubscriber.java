package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.functional.OnEndFunction;
import org.xio.one.reactive.flow.subscribers.internal.functional.OnErrorFunction;
import org.xio.one.reactive.flow.subscribers.internal.functional.OnNextFunction;
import org.xio.one.reactive.flow.subscribers.internal.SubscriberInterface;
import org.xio.one.reactive.flow.subscribers.internal.functional.OnStartFunction;

public class FunctionalStreamItemSubscriber<R, T> {

    private final functionalSubscriber functionalSubscriber;
    private OnNextFunction onNextItem;
    private OnErrorFunction onErrorItem;
    private OnStartFunction onStart;
    private OnEndFunction onEnd;
    private Flow<T, R> trFlow;

    public FunctionalStreamItemSubscriber(Flow<T, R> trFlow) {
        this.trFlow = trFlow;
        this.functionalSubscriber = new functionalSubscriber();
    }

    public <R, T> SubscriberInterface<R, T> subscriber() {
        return (SubscriberInterface) functionalSubscriber;
    }

    private class functionalSubscriber extends StreamItemSubscriber<R, T> {
        @Override
        public void onNext(Item<T, R> itemValue) throws Throwable {
            if (onNextItem != null)
                onNextItem.onNext(itemValue);
        }

        @Override
        public void onError(Throwable error, Item<T, R> itemValue) {
            if (onErrorItem == null)
                super.onError(error, itemValue);
            else
                onErrorItem.onError(error, itemValue);
        }

        @Override
        public void initialise() {
            super.initialise();
            if (onStart!=null)
                onStart.onStart();
        }

        @Override
        public void finalise() {
            if (onEnd!=null)
                onEnd.onEnd();
            else super.finalise();
        }
    }


    public FunctionalStreamItemSubscriber<R, T> onStart(OnStartFunction onStart) {
        this.onStart = onStart;
        return this;
    }

    public FunctionalStreamItemSubscriber<R, T> onEnd(OnEndFunction onEnd) {
        this.onEnd = onEnd;
        return this;
    }

    public FunctionalStreamItemSubscriber<R, T> doOnNext(OnNextFunction<T, R> onNextItem) {
        this.onNextItem = onNextItem;
        return this;
    }

    public FunctionalStreamItemSubscriber<R, T> doOnError(OnErrorFunction<T, R> onErrorItem) {
        this.onErrorItem = onErrorItem;
        return this;
    }

    public FunctionalStreamItemSubscriber<R, T> subscribe() {
        this.trFlow.addSubscriber(functionalSubscriber);
        return this;
    }
}
