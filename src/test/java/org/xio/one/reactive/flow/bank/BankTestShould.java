package org.xio.one.reactive.flow.bank;

import static org.hamcrest.CoreMatchers.is;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xio.one.reactive.flow.XIOService;
import org.xio.one.reactive.flow.bank.api.BankAPI;
import org.xio.one.reactive.flow.bank.domain.Account;
import org.xio.one.reactive.flow.bank.domain.AccountTransaction;
import org.xio.one.reactive.flow.bank.domain.TransactionType;

public class BankTestShould {

  BankAPI bank;

  @BeforeClass
  public static void setup() {
    XIOService.start();
  }

  @AfterClass
  public static void tearDown() {
    XIOService.stop();
  }

  @Before
  public void before() {
    bank = new BankAPI();
  }

  @After
  public void after() {
    bank.close();
  }

  @Test
  public void bankOpensWithLiquidityOfZero() throws Exception {
    Assert.assertThat(bank.calculateLiquidity(), is(0d));
  }

  @Test
  public void bankWithOneAccountWhenOneDepositSubmittedHasLiquidityEqualToDepositOnClose() {
    Account account = bank.newAccount("myaccount");
    bank.submitTransaction(
        new AccountTransaction("cash deposit", null, account.getAccountNumber(), 100d,
            TransactionType.CREDIT));
    Assert.assertThat(bank.calculateLiquidity(), is(100d));
  }

  @Test
  public void bankWithOneAccountWhenTwoTransactionSubmittedHasCorrectLiquidityOnClose()
      throws Exception {
    Account account = bank.newAccount("myaccount");
    bank.submitTransaction(
        new AccountTransaction("cash deposit", null, account.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransaction(
        new AccountTransaction("cash withdrawal", null, account.getAccountNumber(), 200d,
            TransactionType.DEBIT));
    Assert.assertThat(bank.calculateLiquidity(), is(800d));
  }

  @Test
  public void bankWithMultipleAccountsHasCorrectLiquidityOnClose() throws Exception {
    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");
    bank.submitTransaction(
        new AccountTransaction("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransaction(
        new AccountTransaction("cash deposit", null, myaccount1.getAccountNumber(), 200d,
            TransactionType.DEBIT));
    bank.submitTransaction(
        new AccountTransaction("cash deposit", null, myaccount2.getAccountNumber(), 2000d,
            TransactionType.CREDIT));
    bank.submitTransaction(
        new AccountTransaction("cash deposit", null, myaccount2.getAccountNumber(), 200d,
            TransactionType.DEBIT));
    Assert.assertThat(bank.calculateLiquidity(), is(2600d));
  }

  @Test
  public void bankPerformance() throws Exception {
    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");

    int no_transactions = 100000;
    for (int i = 0; i < no_transactions; i++) {
      bank.submitTransaction(
          new AccountTransaction("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
              TransactionType.CREDIT));
      bank.submitTransaction(
          new AccountTransaction("cash withdrawal", null, myaccount1.getAccountNumber(), 200d,
              TransactionType.DEBIT));
      bank.submitTransaction(
          new AccountTransaction("cash deposit", null, myaccount2.getAccountNumber(), 2000d,
              TransactionType.CREDIT));
      bank.submitTransaction(
          new AccountTransaction("cash withdrawal", null, myaccount2.getAccountNumber(), 200d,
              TransactionType.DEBIT));
    }

    Assert.assertThat(bank.calculateLiquidity(), is(2600d * no_transactions));
  }

  @Test
  public void shouldTransferMoniesBetweenTwoAccountsInSameBank() throws Exception {

    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");
    bank.submitTransaction(
        new AccountTransaction("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransaction(
        new AccountTransaction("cash deposit", null, myaccount2.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransaction(new AccountTransaction("cash deposit", myaccount1.getAccountNumber(),
        myaccount2.getAccountNumber(), 500d, TransactionType.TRANSFER));

    Assert.assertThat(bank.getAccountBalance(myaccount1.getAccountNumber()), is(500d));
    Assert.assertThat(bank.getAccountBalance(myaccount2.getAccountNumber()), is(1500d));
    Assert.assertThat(bank.calculateLiquidity(), is(2000d));
  }


}
