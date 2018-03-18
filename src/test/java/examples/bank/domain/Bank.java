package examples.bank.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xio.one.reactive.flow.Flowable;
import org.xio.one.reactive.flow.domain.Item;
import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.core.MultiplexFutureSubscriber;
import org.xio.one.reactive.flow.core.SingleSubscriber;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class Bank {

  HashMap<String, Account> accounts = new HashMap<>();
  Flowable<TransactionRequest, Boolean> itemLoop;
  List<TransactionRequest> bankTransactionLedger = new ArrayList<>();
  Logger logger = LoggerFactory.getLogger(Bank.class);
  MultiplexFutureSubscriber<Boolean, TransactionRequest> ledgerMultiplexFutureSubscriber;

  public Bank() {
    itemLoop = Flow.flow();
  }

  public void open() {
    //Subscriber for every transaction request
    itemLoop.addSingleSubscriber(new SingleSubscriber<Boolean, TransactionRequest>() {

      @Override
      public Optional<Boolean> onNext(TransactionRequest transaction) throws InsufficientFundsException {
        this.processTransaction(transaction);
        return Optional.empty();
      }

      @Override
      public Optional<Object> onError(Throwable error, TransactionRequest itemValue) {
        error.printStackTrace();
        return Optional.empty();
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
      public Map<Long, Boolean> onNext(Stream<Item<TransactionRequest>> e) {
        String multiplexGroupID = UUID.randomUUID().toString();
        e.parallel().forEach(item -> {
          logger.info(
              "itemID" + "|" + item.itemId() +"|" + "groupID" + "|" + multiplexGroupID + "|" + item
                  .value().toString());
          bankTransactionLedger.add(item.value());
          toReturn.put(item.itemId(), true);
        });

        return toReturn;
      }
    };

  }

  public Future<Boolean> submitTransactionRequest(TransactionRequest transaction) {
    return itemLoop.putItem(transaction, ledgerMultiplexFutureSubscriber);
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
    this.itemLoop.end(true);
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
