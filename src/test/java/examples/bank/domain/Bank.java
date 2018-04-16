package examples.bank.domain;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.Flowable;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.subscriber.FutureMultiplexItemSubscriber;
import org.xio.one.reactive.flow.subscriber.SingleItemSubscriber;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Bank {

  HashMap<String, Account> accounts = new HashMap<>();
  Flowable<TransactionRequest, Boolean> transactionEventLoop;
  List<TransactionRequest> bankTransactionLedger = new ArrayList<>();
  Logger logger = Logger.getLogger(Bank.class.getCanonicalName());
  FutureMultiplexItemSubscriber<Boolean, TransactionRequest> ledgerMultiplexFutureSubscriber;

  public Bank() {
    transactionEventLoop = Flow.aFlowable();
  }

  public void open() {
    //Subscriber for every transaction request
    transactionEventLoop.addSingleSubscriber(new SingleItemSubscriber<Boolean, TransactionRequest>() {

      @Override
      public void onNext(FlowItem<TransactionRequest> transaction)
          throws InsufficientFundsException {
        this.processTransaction(transaction.value());
      }

      @Override
      public void onError(Throwable error, FlowItem<TransactionRequest> itemValue) {
        error.printStackTrace();
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

    ledgerMultiplexFutureSubscriber =
        new FutureMultiplexItemSubscriber<Boolean, TransactionRequest>() {
          HashMap<Long, Future<Boolean>> toReturn = new HashMap<>();

          @Override
          public Map<Long, Future<Boolean>> onNext(Stream<FlowItem<TransactionRequest>> e) {
            String multiplexGroupID = UUID.randomUUID().toString();
            e.parallel().forEach(item -> {
              logger.info(
                  "itemID" + "|" + item.itemId() + "|" + "groupID" + "|" + multiplexGroupID + "|"
                      + item.value().toString());
              toReturn.put(item.itemId(), CompletableFuture.completedFuture(bankTransactionLedger.add(item.value())));
            });

            return toReturn;
          }

          @Override
          public void onFutureCompletionError(Throwable error, TransactionRequest itemValue) {
          }

        };

  }

  public Future<Boolean> submitTransactionRequest(TransactionRequest transaction) {
    return transactionEventLoop.putItem(transaction, ledgerMultiplexFutureSubscriber);
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
    this.transactionEventLoop.end(true);
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
