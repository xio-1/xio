package test.bank.domain;

public class InsufficientFundsException extends Throwable {
  public InsufficientFundsException(String message) {
    super(message);
  }
}
