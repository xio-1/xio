package org.xio.one.test.examples.bank.domain;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.flow.ItemFlowable;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public class Bank {

  HashMap<String, Account> accounts = new HashMap<>();

  ItemFlowable<Transaction, Boolean> transactionEventLoop;

  TransactionLedger bankTransactionLedger = new TransactionLedger();

  Logger logger = Logger.getLogger(Bank.class.getCanonicalName());


  public Bank() {
    transactionEventLoop = Flow.anItemFlow("transactions", 10);
  }

  public void open() {
    transactionEventLoop.addSubscriber(new TransactionEventListener(this));
  }

  void creditAccount(String accountNumber, Transaction transaction) {
    Account account = accounts.get(accountNumber);
    account.creditBalance(transaction.amount);
  }

  void debitAccount(String accountNumber, Transaction transaction)
      throws InsufficientFundsException {
    Account account = accounts.get(accountNumber);
    account.debitBalance(transaction.amount);
  }

  public void submitTransactionRequest(Transaction transaction) {
    transactionEventLoop.putItem(transaction);
  }

  public Account newAccount(String name) {
    Account newAccount = new Account(name, UUID.randomUUID().toString());
    accounts.put(newAccount.getAccountNumber(), newAccount);
    return newAccount;
  }

  public Account getAccount(String accountNumber) {
    return accounts.get(accountNumber);
  }

  public void close() {
    this.transactionEventLoop.close(true);
  }

  public Double getLiquidity() throws Exception {
    Double creditTotal = bankTransactionLedger.getAll().stream()
        .filter(t -> t.transactionType.equals(TransactionType.CREDIT))
        .mapToDouble(Transaction::getAmount).sum();
    Double debitTotal = bankTransactionLedger.getAll().stream()
        .filter(t -> t.transactionType.equals(TransactionType.DEBIT))
        .mapToDouble(Transaction::getAmount).sum();
    return creditTotal - debitTotal;
  }
}
