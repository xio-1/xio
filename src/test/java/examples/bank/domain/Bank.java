package examples.bank.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xio.one.stream.event.Event;
import org.xio.one.stream.reactive.AsyncStream;
import org.xio.one.stream.reactive.subscribers.MultiplexFutureSubscriber;
import org.xio.one.stream.reactive.subscribers.SingleSubscriber;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class Bank {

  HashMap<String, Account> accounts = new HashMap<>();
  AsyncStream<TransactionRequest, Boolean> eventLoop;
  List<TransactionRequest> bankTransactionLedger = new ArrayList<>();
  Logger logger = LoggerFactory.getLogger(Bank.class);
  MultiplexFutureSubscriber<Boolean, TransactionRequest> ledgerMultiplexFutureSubscriber;

  public Bank() {
    eventLoop = new AsyncStream<>(UUID.randomUUID().toString());
  }

  public void open() {
    //Subscriber for every transaction request
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

    ledgerMultiplexFutureSubscriber = new MultiplexFutureSubscriber<Boolean, TransactionRequest>() {
      HashMap<Long, Boolean> toReturn = new HashMap<>();

      @Override
      public Map<Long, Boolean> onNext(Stream<Event<TransactionRequest>> e) {
        String multiplexGroupID = UUID.randomUUID().toString();
        e.forEach(event -> {
          logger.info(
              "eventID" + "|" + event.eventId() +"|" + "groupID" + "|" + multiplexGroupID + "|" + event
                  .value().toString());
          bankTransactionLedger.add(event.value());
          toReturn.put(event.eventId(), true);
        });

        return toReturn;
      }
    };

  }

  public Future<Boolean> submitTransactionRequest(TransactionRequest transaction) {
    return eventLoop.putValue(transaction, ledgerMultiplexFutureSubscriber);
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
