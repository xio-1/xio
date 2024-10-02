package org.xio.one.reactive.flow.bank.domain;

public class Account {

  private final String name;
  private final String accountNumber;
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

  public void debitBalance(double amount) throws InsufficientFundsException {
    if (amount > balance) {
      throw new InsufficientFundsException(
          "Insufficient funds available for debit amount " + amount);
    }
    balance -= amount;
  }
}
