package test;

import org.junit.Test;
import org.xio.one.stream.reactive.AsyncStream;
import org.xio.one.stream.reactive.subscribers.ContinuousSingleEventSubscriber;

import java.util.HashMap;
import java.util.UUID;

public class EventLoopExample {

  public class Bank {

    HashMap<String, Account> accounts = new HashMap<>();
    AsyncStream<Transaction, Boolean> transaction_stream;

    public Bank() {
      transaction_stream = new AsyncStream<>("transaction_stream");
    }

    public void open() {
      transaction_stream.withSubscriber(new ContinuousSingleEventSubscriber<Boolean, Transaction>() {
        @Override
        public Boolean process(Transaction transaction) {
          if (transaction.transactionType==TransactionType.CREDIT)
            creditAccount(transaction.toAccount,transaction);
          return true;
        }
      });
    }

    public void processTransaction(Transaction transaction) {
      transaction_stream.putValue(transaction);
    }

    public Account newAccount(String name) {
      Account newAccount = new Account(name, UUID.randomUUID().toString());
      accounts.put(newAccount.getAccountNumber(), newAccount);
      return newAccount;
    }

    public void creditAccount(String accountNumber, Transaction transaction) {
      Account account = accounts.get(accountNumber);
      account.creditBalance(transaction.amount);
    }

    public void close() {
      this.transaction_stream.end(true);
    }
  }

  public class Account {
    private String name;
    private String accountNumber;
    private double balance;

    public Account(String name, String accountNumber) {
      this.name = name;
      this.accountNumber = accountNumber;
      this.balance = 0;
    }

    public double getBalance() {
      return balance;
    }

    public void creditBalance(double amount) {
      balance += amount;
    }

    public String getName() {
      return name;
    }

    public String getAccountNumber() {
      return accountNumber;
    }
  }

  public enum TransactionType {
    CREDIT,
    DEBIT,
    TRANSFER
  }

  public class Transaction {
    String reference;
    String fromAccount;
    String toAccount;
    TransactionType transactionType;
    double amount;

    public Transaction(
        String fromReference,
        String fromAccount,
        String accountTo,
        double amount,
        TransactionType transactionType) {
      this.amount = amount;
      this.reference = fromReference;
      this.toAccount = accountTo;
      this.fromAccount = fromAccount;
      this.transactionType = transactionType;
    }

    public double getAmount() {
      return amount;
    }
  }

  @Test
  public void accountTest() throws Exception {
    Bank bank = new Bank();
    bank.open();
    Account depositAccount = bank.newAccount("deposit");
    bank.processTransaction(
        new Transaction(
            "cash deposit", null, depositAccount.accountNumber, 100.00, TransactionType.CREDIT));
    bank.processTransaction(
        new Transaction(
            "cash deposit", null, depositAccount.accountNumber, 100.00, TransactionType.CREDIT));
    bank.close();
    System.out.println(bank.accounts.get(depositAccount.accountNumber).balance);
  }
}
