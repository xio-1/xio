package examples.bank.domain;

public class TransactionRequest {

  final Long timestamp;
  final String reference;
  final String fromAccount;
  final String toAccount;
  final TransactionType transactionType;
  final double amount;

  public TransactionRequest(String fromReference, String fromAccount, String accountTo, double amount,
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
    final StringBuffer sb = new StringBuffer("TransactionRequest{");
    sb.append("timestamp=").append(timestamp);
    sb.append(", reference='").append(reference).append('\'');
    sb.append(", fromAccount='").append(fromAccount).append('\'');
    sb.append(", toAccount='").append(toAccount).append('\'');
    sb.append(", transactionType=").append(transactionType);
    sb.append(", amount=").append(amount);
    sb.append('}');
    return sb.toString();
  }
}
