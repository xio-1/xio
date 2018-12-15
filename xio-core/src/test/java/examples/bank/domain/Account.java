package examples.bank.domain;

import java.util.ArrayList;
import java.util.List;

public class Account {
  private String name;
  private String accountNumber;
  private double balance;
  private List<TransactionRequest> transactions;

  public Account(String name, String accountNumber) {
    this.name = name;
    this.accountNumber = accountNumber;
    this.balance = 0;
    this.transactions = new ArrayList<>();
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

  public void debitBalance(double amount) throws InsufficientFundsException {
    if (amount>balance)
      throw new InsufficientFundsException("Insufficient funds available for debit amount " + amount);
    balance -= amount;
  }
}
