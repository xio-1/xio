package examples.bank;

import examples.bank.domain.Account;
import examples.bank.domain.Bank;
import examples.bank.domain.TransactionRequest;
import examples.bank.domain.TransactionType;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;

public class BankTestShould {

  @Test
  public void bankOpensWithLiquidityOfZero() throws Exception {
    Bank bank = new Bank();
    bank.open();
    bank.close();
    Assert.assertThat(bank.getLiquidity(), is(0d));
  }

  @Test
  public void bankWithOneAccountWhenOneDepositSubmittedHasLiquidityEqualToDepositOnClose() {
    Bank bank = new Bank();
    bank.open();
    Account account = bank.newAccount("myaccount");
    bank.submitTransactionRequest(
        new TransactionRequest("cash deposit", null, account.getAccountNumber(), 100d,
            TransactionType.CREDIT));
    bank.close();
    Assert.assertThat(bank.getLiquidity(), is(100d));
  }

  @Test
  public void bankWithOneAccountWhenTwoTransactionSubmittedHasCorrectLiquidityOnClose() {
    Bank bank = new Bank();
    bank.open();
    Account account = bank.newAccount("myaccount");
    bank.submitTransactionRequest(
        new TransactionRequest("cash deposit", null, account.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransactionRequest(
        new TransactionRequest("cash withdrawal", null, account.getAccountNumber(), 200d,
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
    Future<Boolean> t1 = bank.submitTransactionRequest(
        new TransactionRequest("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    Future<Boolean> t2 = bank.submitTransactionRequest(
        new TransactionRequest("cash deposit", null, myaccount1.getAccountNumber(), 200d,
            TransactionType.DEBIT));
    Future<Boolean> t3 = bank.submitTransactionRequest(
        new TransactionRequest("cash deposit", null, myaccount2.getAccountNumber(), 2000d,
            TransactionType.CREDIT));
    Future<Boolean> t4 = bank.submitTransactionRequest(
        new TransactionRequest("cash deposit", null, myaccount2.getAccountNumber(), 200d,
            TransactionType.DEBIT));
    if (t1.get() && t2.get() && t3.get() && t4.get())
      bank.close();
    Assert.assertThat(bank.getLiquidity(), is(2600d));
  }

  @Test
  public void bankPerformance() throws Exception {
    Bank bank = new Bank();
    bank.open();
    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");

    Future<Boolean> t1 = null;
    Future<Boolean> t2 = null;
    Future<Boolean> t3 = null;
    Future<Boolean> t4 = null;
    for (int i = 0; i < 10000; i++) {
      t1 = bank.submitTransactionRequest(
          new TransactionRequest("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
              TransactionType.CREDIT));
      t2 = bank.submitTransactionRequest(
          new TransactionRequest("cash deposit", null, myaccount1.getAccountNumber(), 200d,
              TransactionType.DEBIT));
      t3 = bank.submitTransactionRequest(
          new TransactionRequest("cash deposit", null, myaccount2.getAccountNumber(), 2000d,
              TransactionType.CREDIT));
      t4 = bank.submitTransactionRequest(
          new TransactionRequest("cash deposit", null, myaccount2.getAccountNumber(), 200d,
              TransactionType.DEBIT));
    }
    if (t1.get() && t2.get() && t3.get() && t4.get())
    bank.close();
    Assert.assertThat(bank.getLiquidity(), is(2600d * 10000d));
  }

  @Test
  public void shouldTransferMoniesBetweenTwoAccountsInSameBank() {
    Bank bank = new Bank();
    bank.open();
    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");
    bank.submitTransactionRequest(
        new TransactionRequest("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransactionRequest(
        new TransactionRequest("cash deposit", null, myaccount2.getAccountNumber(), 1000d,
            TransactionType.CREDIT));
    bank.submitTransactionRequest(
        new TransactionRequest("cash deposit", myaccount1.getAccountNumber(),
            myaccount2.getAccountNumber(), 500d, TransactionType.TRANSFER));
    bank.close();
    Assert.assertThat(myaccount1.getBalance(), is(500d));
    Assert.assertThat(myaccount2.getBalance(), is(1500d));
    Assert.assertThat(bank.getLiquidity(), is(2000d));
  }


}
