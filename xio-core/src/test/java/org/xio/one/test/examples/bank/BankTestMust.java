package org.xio.one.test.examples.bank;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.test.examples.bank.domain.Account;
import org.xio.one.test.examples.bank.domain.Bank;
import org.xio.one.test.examples.bank.domain.Transaction;
import org.xio.one.test.examples.bank.domain.TransactionType;

import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;

public class BankTestMust {

  Logger logger = Logger.getLogger(BankTestMust.class.getCanonicalName());

  @Test
  public void bankOpensWithLiquidityOfZero() throws Exception {
    Bank bank = new Bank();
    bank.open();
    bank.close();
    Assert.assertThat(bank.getLiquidity(), is(0d));
  }

  @Test
  public void bankWithOneAccountWhenOneDepositSubmittedHasLiquidityEqualToDepositOnClose()
      throws Exception {
    Bank bank = new Bank();
    bank.open();
    Account account = bank.newAccount("myaccount");
    bank.submitTransactionRequest(
        new Transaction("cash deposit", null, account.getAccountNumber(), 100d,
            TransactionType.CREDIT));
    bank.close();
    Assert.assertThat(bank.getLiquidity(), is(100d));
  }

  @Test
  public void bankWithOneAccountWhenTwoTransactionSubmittedHasCorrectLiquidityOnClose()
      throws Exception {
    Bank bank = new Bank();
    bank.open();
    Account account = bank.newAccount("myaccount");
    bank.submitTransactionRequest(
        new Transaction("cash deposit", null, account.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransactionRequest(
        new Transaction("cash withdrawal", null, account.getAccountNumber(), 200d,
            TransactionType.DEBIT));
    bank.close();

    Assert.assertThat(bank.getLiquidity(), is(800d));
  }

  @Test
  public void bankWithMultipleAccountsHasCorrectLiquidityOnClose() throws Exception {
    Bank bank = new Bank();
    bank.open();
    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");
    bank.submitTransactionRequest(
        new Transaction("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransactionRequest(
        new Transaction("cash deposit", null, myaccount1.getAccountNumber(), 200d,
            TransactionType.DEBIT));
    bank.submitTransactionRequest(
        new Transaction("cash deposit", null, myaccount2.getAccountNumber(), 2000d,
            TransactionType.CREDIT));
    bank.submitTransactionRequest(
        new Transaction("cash deposit", null, myaccount2.getAccountNumber(), 200d,
            TransactionType.DEBIT));

    bank.close();
    Assert.assertThat(bank.getLiquidity(), is(2600d));
  }

  @Test
  public void bankPerformance() throws Exception {
    Bank bank = new Bank();
    bank.open();
    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");

    int no_transactions = 10000;
    for (int i = 0; i < no_transactions; i++) {
      bank.submitTransactionRequest(
          new Transaction("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
              TransactionType.CREDIT));
      bank.submitTransactionRequest(
          new Transaction("cash withdrawal", null, myaccount1.getAccountNumber(), 200d,
              TransactionType.DEBIT));
      bank.submitTransactionRequest(
          new Transaction("cash deposit", null, myaccount2.getAccountNumber(), 2000d,
              TransactionType.CREDIT));
      bank.submitTransactionRequest(
          new Transaction("cash withdrawal", null, myaccount2.getAccountNumber(), 200d,
              TransactionType.DEBIT));
    }

    bank.close();
    Assert.assertThat(bank.getLiquidity(), is(2600d * no_transactions));
  }

  @Test
  public void shouldTransferMoniesBetweenTwoAccountsInSameBank() throws Exception {
    Bank bank = new Bank();
    bank.open();
    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");
    bank.submitTransactionRequest(
        new Transaction("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransactionRequest(
        new Transaction("cash deposit", null, myaccount2.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransactionRequest(
        new Transaction("cash deposit", myaccount1.getAccountNumber(),
            myaccount2.getAccountNumber(), 500d, TransactionType.TRANSFER));
    bank.close();

    Assert.assertThat(myaccount1.getBalance(), is(500d));
    Assert.assertThat(myaccount2.getBalance(), is(1500d));
    Assert.assertThat(bank.getLiquidity(), is(2000d));
  }


}
