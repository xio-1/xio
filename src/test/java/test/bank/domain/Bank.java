package test.bank.domain;

import org.xio.one.stream.reactive.AsyncStream;
import org.xio.one.stream.reactive.subscribers.OnNextSubscriber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Bank {

  HashMap<String, Account> accounts = new HashMap<>();
  AsyncStream<Transaction, Boolean> transaction_stream;
  List<Transaction> bankTransactionLedger = new ArrayList<>();

  public Bank() {
    transaction_stream = new AsyncStream<>("transaction_stream");
  }

  public void open() {
    transaction_stream.withStreamSubscriber(new OnNextSubscriber<Boolean, Transaction>() {
      @Override
      public Boolean onNext(Transaction transaction) throws InsufficientFundsException {
        Bank.this.processTransaction(transaction);
        return true;
      }

      @Override
      public Object onError(Throwable error, Transaction eventValue) {
        error.printStackTrace();
        return error;
      }
    });
  }

  private void processTransaction(Transaction transaction) throws InsufficientFundsException {
    if (transaction.transactionType == TransactionType.CREDIT)
      creditAccount(transaction.toAccount, transaction);
    else if (transaction.transactionType == TransactionType.DEBIT)
      debitAccount(transaction.toAccount, transaction);
    else if (transaction.transactionType == TransactionType.TRANSFER) {
      debitAccount(transaction.fromAccount, transaction);
      creditAccount(transaction.toAccount, transaction);
    }
    bankTransactionLedger.add(transaction);
  }

  public void submitTransaction(Transaction transaction) {
    transaction_stream.putValue(transaction);
  }

  private void creditAccount(String accountNumber, Transaction transaction) {
    Account account = accounts.get(accountNumber);
    account.creditBalance(transaction.amount);
  }

  private void debitAccount(String accountNumber, Transaction transaction)
      throws InsufficientFundsException {
    Account account = accounts.get(accountNumber);
    account.debitBalance(transaction.amount);
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
    this.transaction_stream.end(true);
  }

  public Double getLiquidity() {
    Double creditTotal = this.bankTransactionLedger.stream()
        .filter(t -> t.transactionType.equals(TransactionType.CREDIT))
        .mapToDouble(Transaction::getAmount).sum();
    Double debitTotal = this.bankTransactionLedger.stream()
        .filter(t -> t.transactionType.equals(TransactionType.DEBIT))
        .mapToDouble(Transaction::getAmount).sum();
    return creditTotal - debitTotal;
  }
}
