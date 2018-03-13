package test.bank.domain;

public class Transaction {

  final Long timestamp;
  final String reference;
  final String fromAccount;
  final String toAccount;
  final TransactionType transactionType;
  final double amount;

  public Transaction(String fromReference, String fromAccount, String accountTo, double amount,
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
}
