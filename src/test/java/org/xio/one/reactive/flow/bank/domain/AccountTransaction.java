package org.xio.one.reactive.flow.bank.domain;

import java.util.Objects;

public class AccountTransaction {

  final Long timestamp;
  final String reference;
  final String fromAccount;
  final String toAccount;
  final TransactionType transactionType;
  final double amount;

  public AccountTransaction(String fromReference, String fromAccount, String accountTo,
      double amount,
      TransactionType transactionType) {
    this.amount = amount;
    this.reference = fromReference;
    this.toAccount = accountTo;
    this.fromAccount = fromAccount;
    this.transactionType = transactionType;
    this.timestamp = System.currentTimeMillis();
  }

  public double getAmount() {
    return amount;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public String getReference() {
    return reference;
  }

  public String getFromAccount() {
    return fromAccount;
  }

  public String getToAccount() {
    return toAccount;
  }

  public TransactionType getTransactionType() {
    return transactionType;
  }

  @Override
  public String toString() {
    String sb = "TransactionRequest{" + "timestamp=" + timestamp +
        ", reference='" + reference + '\'' +
        ", fromAccount='" + fromAccount + '\'' +
        ", toAccount='" + toAccount + '\'' +
        ", transactionType=" + transactionType +
        ", amount=" + amount +
        '}';
    return sb;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccountTransaction that = (AccountTransaction) o;
    return Double.compare(that.amount, amount) == 0 && Objects.equals(timestamp, that.timestamp)
        && Objects.equals(reference, that.reference) && Objects
        .equals(fromAccount, that.fromAccount) && Objects.equals(toAccount, that.toAccount)
        && transactionType == that.transactionType;
  }

  @Override
  public int hashCode() {

    return Objects.hash(timestamp, reference, fromAccount, toAccount, transactionType, amount);
  }
}
