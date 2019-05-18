package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;
import org.xio.one.reactive.flow.subscribers.internal.functional.*;

import java.util.function.Predicate;

public class FunctionalSubscriber<R, T> {

  private Subscriber<R, T> functionalSubscriber = null;
  private OnNextFunction<T, R> onNextItem;
  private OnNextReturnFunction<T, R> onNextItemReturn;
  private OnErrorFunction onErrorItem;
  private OnStartFunction onStart;
  private OnEndReturnFunction<R> onEndReturn;
  private OnEndFunction onEndFunction;

  private Flow<T, R> trFlow;
  private OnEndFunction onEnd;
  private OnExitAndReturnFunction<R> onExitAndReturn;
  private Predicate<T> exitPredicate;

  public FunctionalSubscriber(Flow<T, R> trFlow, Class<Subscriber<R, T>> subscriberClass) {
    this.trFlow = trFlow;
    if (subscriberClass.isAssignableFrom(ItemSubscriber.class))
      this.functionalSubscriber = new FunctionalItemSubscriber();
    else if (subscriberClass.isAssignableFrom(FutureItemSubscriber.class))
      this.functionalSubscriber = new FunctionalFutureItemSubscriber();
  }

  public <R, T> Subscriber<R, T> subscriber() {
    return (Subscriber) functionalSubscriber;
  }

  public FunctionalSubscriber<R, T> onStart(OnStartFunction onStart) {
    this.onStart = onStart;
    return this;
  }

  public FunctionalSubscriber<R, T> finallyOnEnd(OnEndFunction onEnd) {
    this.onEnd = onEnd;
    return this;
  }

  public FunctionalSubscriber<R, T> andOnEndReturn(OnEndReturnFunction onEndReturn) {
    this.onEndReturn = onEndReturn;
    return this;
  }

  public FunctionalSubscriber<R, T> whenPredicateExitAndReturn(Predicate<T> exitPredicate,
      OnExitAndReturnFunction<R> onExitAndReturn) {
    this.onExitAndReturn = onExitAndReturn;
    this.exitPredicate = exitPredicate;
    return this;
  }

  public FunctionalSubscriber<R, T> doForEach(OnNextFunction<T, R> onNextItem) {
    this.onNextItem = onNextItem;
    return this;
  }

  public FunctionalSubscriber<R, T> returnForEach(OnNextReturnFunction<T, R> onNextItemReturn) {
    this.onNextItemReturn = onNextItemReturn;
    return this;
  }

  public FunctionalSubscriber<R, T> doOnError(OnErrorFunction<T, R> onErrorItem) {
    this.onErrorItem = onErrorItem;
    return this;
  }

  public Subscriber<R, T> subscribe() {
    this.trFlow.addSubscriber(functionalSubscriber);
    return functionalSubscriber;
  }

  private class FunctionalItemSubscriber extends ItemSubscriber<R, T> {
    @Override
    public void onNext(Item<T, R> item) {
      if (onExitAndReturn != null && exitPredicate.test(item.value()))
        exitAndReturn(onExitAndReturn.onExit());
      if (onNextItem != null)
        onNextItem.onNext(item);
      if (onNextItemReturn != null)
        throw new UnsupportedOperationException("ItemSubscriber does not support returning a "
            + "result per item, use a FutureItemSubscriber instead");
    }

    @Override
    public void onError(Throwable error, Item<T, R> item) {
      if (onErrorItem == null)
        super.onError(error, item);
      else
        onErrorItem.onError(error, item);
    }

    @Override
    public void initialise() {
      if (onStart != null)
        onStart.onStart();
      else
        super.initialise();
    }

    @Override
    public R finalise() {
      if (onEndReturn != null)
        return onEndReturn.onEnd();
      else if (onEnd != null)
        onEnd.onEnd();
      return super.finalise();
    }
  }


  private class FunctionalFutureItemSubscriber extends FutureItemSubscriber<R, T> {
    @Override
    public R onNext(Item<T, R> item) {
      if (onExitAndReturn != null && exitPredicate.test(item.value()))
        exitAndReturn(onExitAndReturn.onExit());
      if (onNextItemReturn != null)
        return onNextItemReturn.onNext(item);
      if (onNextItem != null)
        throw new UnsupportedOperationException("ItemSubscriber does not support returning a "
            + "result per item, use a FutureItemSubscriber instead");
      return null;
    }

    @Override
    public void onError(Throwable error, Item<T, R> item) {
      if (onErrorItem != null)
        onErrorItem.onError(error, item);
    }

    @Override
    public void initialise() {
      super.initialise();
      if (onStart != null)
        onStart.onStart();
    }

    @Override
    public R finalise() {
      if (onEndReturn != null)
        return onEndReturn.onEnd();
      else if (onEnd != null)
        onEnd.onEnd();
      return super.finalise();
    }
  }
}
