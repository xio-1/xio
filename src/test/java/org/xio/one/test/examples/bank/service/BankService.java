package org.xio.one.test.examples.bank.service;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.ItemSubscriber;
import org.xio.one.test.examples.bank.domain.*;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class BankService extends ItemSubscriber<Boolean, AccountTransaction> {

  private HashMap<String, Account> accounts = new HashMap<>();
  private TransactionLedger transactionLedger = new TransactionLedger();
  private ItemFlowable<AccountTransaction, Boolean> transactionEventLoop;
  private AtomicInteger await = new AtomicInteger(0);

  public BankService() {
    transactionEventLoop = Flow.anItemFlow("transactions", 10);
    transactionEventLoop.addSubscriber(this);
  }

  public Account newAccount(String name) {
    Account newAccount = new Account(name, UUID.randomUUID().toString());
    accounts.put(newAccount.getAccountNumber(), newAccount);
    return newAccount;
  }

  public Account getAccount(String accountNumber) {
    return accounts.get(accountNumber);
  }

  public double getAccountBalance(String accountNumber) {
    await();
    return accounts.get(accountNumber).getBalance();
  }

  public void submitTransaction(AccountTransaction transaction) {
    await.incrementAndGet();
    transactionEventLoop.putItem(transaction);
  }

  private void await() {
    while (await.getPlain()>0)
      LockSupport.parkNanos(10000);
  }

  public Double calculateLiquidity() {
    await();
    Double creditTotal = getTransactionLedger().getAll().stream()
        .filter(t -> t.getTransactionType().equals(TransactionType.CREDIT))
        .mapToDouble(AccountTransaction::getAmount).sum();
    Double debitTotal = getTransactionLedger().getAll().stream()
        .filter(t -> t.getTransactionType().equals(TransactionType.DEBIT))
        .mapToDouble(AccountTransaction::getAmount).sum();
    return creditTotal - debitTotal;
  }

  @Override
  public void onNext(Item<AccountTransaction> item) {
    AccountTransaction transaction = item.value();
    if (transaction.getTransactionType().equals(TransactionType.CREDIT))
      creditAccount(transaction.getToAccount(), transaction);
    else if (transaction.getTransactionType().equals(TransactionType.DEBIT))
      debitAccount(transaction.getToAccount(), transaction);
    else if (transaction.getTransactionType().equals(TransactionType.TRANSFER)) {
      debitAccount(transaction.getFromAccount(), transaction);
      creditAccount(transaction.getToAccount(), transaction);
    }
    transactionLedger.add(transaction);
    await.decrementAndGet();
  }

  private void creditAccount(String accountNumber, AccountTransaction transaction) {
    Account account = accounts.get(accountNumber);
    account.creditBalance(transaction.getAmount());
  }

  private void debitAccount(String accountNumber, AccountTransaction transaction)
      throws InsufficientFundsException {
    Account account = accounts.get(accountNumber);
    account.debitBalance(transaction.getAmount());
  }

  private TransactionLedger getTransactionLedger() {
    return transactionLedger;
  }

  public void close() {
    transactionEventLoop.close(true);
  }

}
