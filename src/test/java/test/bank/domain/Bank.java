package test.bank.domain;

import org.xio.one.stream.reactive.AsyncStream;
import org.xio.one.stream.reactive.subscribers.SingleSubscriber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Bank {

  HashMap<String, Account> accounts = new HashMap<>();
  AsyncStream<TransactionRequest, Boolean> eventLoop;
  List<TransactionRequest> bankTransactionLedger = new ArrayList<>();

  public Bank() {
    eventLoop = new AsyncStream<>("eventLoop");
  }

  public void open() {
    eventLoop.withSingleSubscriber(new SingleSubscriber<Boolean, TransactionRequest>() {

      @Override
      public Boolean onNext(TransactionRequest transaction) throws InsufficientFundsException {
        this.processTransaction(transaction);
        return true;
      }

      @Override
      public Object onError(Throwable error, TransactionRequest eventValue) {
        error.printStackTrace();
        return error;
      }

      private void processTransaction(TransactionRequest transaction)
          throws InsufficientFundsException {
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

      private void creditAccount(String accountNumber, TransactionRequest transaction) {
        Account account = accounts.get(accountNumber);
        account.creditBalance(transaction.amount);
      }

      private void debitAccount(String accountNumber, TransactionRequest transaction)
          throws InsufficientFundsException {
        Account account = accounts.get(accountNumber);
        account.debitBalance(transaction.amount);
      }

    });
  }

  public void submitTransactionRequest(TransactionRequest transaction) {
    eventLoop.putValue(transaction);
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
    this.eventLoop.end(true);
  }

  public Double getLiquidity() {
    Double creditTotal = this.bankTransactionLedger.stream()
        .filter(t -> t.transactionType.equals(TransactionType.CREDIT))
        .mapToDouble(TransactionRequest::getAmount).sum();
    Double debitTotal = this.bankTransactionLedger.stream()
        .filter(t -> t.transactionType.equals(TransactionType.DEBIT))
        .mapToDouble(TransactionRequest::getAmount).sum();
    return creditTotal - debitTotal;
  }
}
