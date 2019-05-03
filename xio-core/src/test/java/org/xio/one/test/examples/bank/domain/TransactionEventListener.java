package org.xio.one.test.examples.bank.domain;

import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class TransactionEventListener extends ItemSubscriber<Boolean, Transaction> {

  Logger logger = Logger.getLogger(TransactionEventListener.class.getName());

  private Bank bank;

  public TransactionEventListener(Bank bank) {
    this.bank = bank;
  }

  @Override
  public void onNext(Item<Transaction, Boolean> transaction)
      throws InsufficientFundsException {
    if (this.processTransaction(transaction.value()))
      recordInLedger(transaction.value());
    else
      throw new RuntimeException("Error processing payment");
  }

  @Override
  public void onError(Throwable error, Item<Transaction, Boolean> itemValue) {
    error.printStackTrace();
  }

  private void recordInLedger(Transaction transaction) {
    logger.info("recording transaction in ledger" + transaction.toString());
    bank.bankTransactionLedger.add(transaction);
  }

  private boolean processTransaction(Transaction transaction)
      throws InsufficientFundsException {
    if (transaction.transactionType == TransactionType.CREDIT)
      bank.creditAccount(transaction.toAccount, transaction);
    else if (transaction.transactionType == TransactionType.DEBIT)
      bank.debitAccount(transaction.toAccount, transaction);
    else if (transaction.transactionType == TransactionType.TRANSFER) {
      bank.debitAccount(transaction.fromAccount, transaction);
      bank.creditAccount(transaction.toAccount, transaction);
    }
    return true;
  }


}
