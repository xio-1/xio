package examples.bank.domain;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.FlowItem;
import org.xio.one.reactive.flow.domain.SimpleFlowable;
import org.xio.one.reactive.flow.subscriber.StreamItemSubscriber;
import org.xio.one.reactive.flow.subscriber.StreamMultiplexItemSubscriber;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Bank {

  HashMap<String, Account> accounts = new HashMap<>();
  SimpleFlowable<TransactionRequest, Boolean> transactionEventLoop;
  SimpleFlowable<TransactionRequest, Boolean> transactionLedger;

  List<TransactionRequest> bankTransactionLedger = Collections.synchronizedList(new ArrayList<>());
  Logger logger = Logger.getLogger(Bank.class.getCanonicalName());
  StreamMultiplexItemSubscriber<Boolean, TransactionRequest> ledgerMultiplexFutureSubscriber;

  public Bank() {
    transactionEventLoop = Flow.aSimpleFlowable();
  }

  public void open() {
    //Subscriber for every transaction request
    transactionEventLoop.addSubscriber(new StreamItemSubscriber<>() {

      @Override
      public void onNext(FlowItem<TransactionRequest> transaction)
          throws InsufficientFundsException, ExecutionException, InterruptedException {
        if (this.processTransaction(transaction.value()))
          recordInLedger(transaction.value());
        else throw new RuntimeException("Error processing payment");
      }

      @Override
      public void onError(Throwable error, FlowItem<TransactionRequest> itemValue) {
        error.printStackTrace();
      }

      private void recordInLedger(TransactionRequest transaction)
      {
        transactionLedger.putItem(transaction);
      }

      private boolean processTransaction(TransactionRequest transaction)
          throws InsufficientFundsException {
        if (transaction.transactionType == TransactionType.CREDIT)
          creditAccount(transaction.toAccount, transaction);
        else if (transaction.transactionType == TransactionType.DEBIT)
          debitAccount(transaction.toAccount, transaction);
        else if (transaction.transactionType == TransactionType.TRANSFER) {
          debitAccount(transaction.fromAccount, transaction);
          creditAccount(transaction.toAccount, transaction);
        }
        return true;
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

    transactionLedger = Flow.aSimpleFlowable("ledger");
    transactionLedger.addSubscriber(new StreamMultiplexItemSubscriber<Boolean, TransactionRequest>() {
      @Override
      public void onNext(Stream<FlowItem<TransactionRequest>> e) {

        String multiplexGroupID = UUID.randomUUID().toString();
        e.forEach(item -> {
          logger.info(
              "itemID" + "|" + item.itemId() + "|" + "groupID" + "|" + multiplexGroupID + "|" + item
                  .value().toString());
          bankTransactionLedger.add(item.value());
        });
      }

      @Override
      public void finalise() {
        super.finalise();
      }
    });
  }

  public void submitTransactionRequest(TransactionRequest transaction) {
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
    this.transactionEventLoop.end(true);
    this.transactionLedger.end(true);
  }

  public Double getLiquidity() throws Exception {
    TransactionRequest[] transactionRequests = this.bankTransactionLedger.toArray(new TransactionRequest[0]);
    Double creditTotal = Arrays.asList(transactionRequests).stream()
        .filter(t -> t.transactionType.equals(TransactionType.CREDIT))
        .mapToDouble(TransactionRequest::getAmount).sum();
    Double debitTotal =  Arrays.asList(transactionRequests).stream()
        .filter(t -> t.transactionType.equals(TransactionType.DEBIT))
        .mapToDouble(TransactionRequest::getAmount).sum();
    return creditTotal - debitTotal;
  }
}
