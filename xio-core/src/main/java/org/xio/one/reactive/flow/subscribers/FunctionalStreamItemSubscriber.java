package org.xio.one.reactive.flow.subscribers;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;
import org.xio.one.reactive.flow.subscribers.internal.functional.*;

import java.util.function.Predicate;

public class FunctionalStreamItemSubscriber<R, T> {

  private final functionalSubscriber functionalSubscriber;
  private OnNextFunction onNextItem;
  private OnErrorFunction onErrorItem;
  private OnStartFunction onStart;
  private OnEndReturnFunction<R> onEndReturn;
  private OnEndFunction onEndFunction;
  private Flow<T, R> trFlow;
  private OnEndFunction onEnd;
  private OnExitAndReturnFunction<R> onExitAndReturn;
  private Predicate<T> exitPredicate;

  public FunctionalStreamItemSubscriber(Flow<T, R> trFlow) {
    this.trFlow = trFlow;
    this.functionalSubscriber = new functionalSubscriber();
  }

  public <R, T> Subscriber<R, T> subscriber() {
    return (Subscriber) functionalSubscriber;
  }

  public FunctionalStreamItemSubscriber<R, T> onStart(OnStartFunction onStart) {
    this.onStart = onStart;
    return this;
  }

  public FunctionalStreamItemSubscriber<R, T> onEnd(OnEndFunction onEnd) {
    this.onEnd = onEnd;
    return this;
  }

  public FunctionalStreamItemSubscriber<R, T> onEndReturn(OnEndReturnFunction onEndReturn) {
    this.onEndReturn = onEndReturn;
    return this;
  }

  public FunctionalStreamItemSubscriber<R, T> ifPredicateExitAndReturn(Predicate<T> exitPredicate,
      OnExitAndReturnFunction<R> onExitAndReturn) {
    this.onExitAndReturn = onExitAndReturn;
    this.exitPredicate = exitPredicate;
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

  public Subscriber<R, T> subscribe() {
    this.trFlow.addSubscriber(functionalSubscriber);
    return functionalSubscriber;
  }

  private class functionalSubscriber extends FlowItemSubscriber<R, T> {
    @Override
    public void onNext(Item<T, R> itemValue) throws Throwable {
      if (onNextItem != null)
        onNextItem.onNext(itemValue);
      if (onExitAndReturn != null && exitPredicate.test(itemValue.value()))
        exitAndReturn(onExitAndReturn.onExit());
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
