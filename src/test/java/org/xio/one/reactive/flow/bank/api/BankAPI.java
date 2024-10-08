package org.xio.one.reactive.flow.bank.api;

import org.xio.one.reactive.flow.bank.domain.Account;
import org.xio.one.reactive.flow.bank.domain.AccountTransaction;
import org.xio.one.reactive.flow.bank.service.BankService;

public class BankAPI {

  BankService bankService;

  public BankAPI() {
    bankService = new BankService();
  }

  public Account newAccount(String name) {
    return bankService.newAccount(name);
  }

  public Account getAccount(String accountNumber) {
    return bankService.getAccount(accountNumber);
  }

  public double getAccountBalance(String accountNumber) {
    return bankService.getAccountBalance(accountNumber);
  }

  public void submitTransaction(AccountTransaction transaction) {
    bankService.submitTransaction(transaction);
  }

  public Double calculateLiquidity() {
    return bankService.calculateLiquidity();

  }

  public void close() {
    this.bankService.close();
  }

}
