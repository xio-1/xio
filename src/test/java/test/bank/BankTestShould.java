package test.bank;

import org.junit.Assert;
import org.junit.Test;
import test.bank.domain.Account;
import test.bank.domain.Bank;
import test.bank.domain.Transaction;
import test.bank.domain.TransactionType;

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
    bank.submitTransaction(new Transaction("cash deposit", null, account.getAccountNumber(), 100d,
        TransactionType.CREDIT));
    bank.close();
    Assert.assertThat(bank.getLiquidity(), is(100d));
  }

  @Test
  public void bankWithOneAccountWhenTwoTransactionSubmittedHasCorrectLiquidityOnClose() {
    Bank bank = new Bank();
    bank.open();
    Account account = bank.newAccount("myaccount");
    bank.submitTransaction(new Transaction("cash deposit", null, account.getAccountNumber(), 1000d,
        TransactionType.CREDIT));
    bank.submitTransaction(new Transaction("cash withdrawal", null, account.getAccountNumber(), 200d,
        TransactionType.DEBIT));
    bank.close();
    Assert.assertThat(bank.getLiquidity(), is(800d));
  }

  @Test
  public void bankWithMultipleAccountsHasCorrectLiquidityOnClose() {
    Bank bank = new Bank();
    bank.open();
    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");
    bank.submitTransaction(new Transaction("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
        TransactionType.CREDIT));
    bank.submitTransaction(new Transaction("cash deposit", null, myaccount1.getAccountNumber(), 200d,
        TransactionType.DEBIT));
    bank.submitTransaction(new Transaction("cash deposit", null, myaccount2.getAccountNumber(), 2000d,
        TransactionType.CREDIT));
    bank.submitTransaction(new Transaction("cash deposit", null, myaccount2.getAccountNumber(), 200d,
        TransactionType.DEBIT));
    bank.close();

    Assert.assertThat(bank.getLiquidity(), is(2600d));
  }

  @Test
  public void shouldTransferMoniesBetweenTwoAccountsInSameBank() {
    Bank bank = new Bank();
    bank.open();
    Account myaccount1 = bank.newAccount("myaccount1");
    Account myaccount2 = bank.newAccount("myaccount2");
    bank.submitTransaction(new Transaction("cash deposit", null, myaccount1.getAccountNumber(), 1000d,
        TransactionType.CREDIT));
    bank.submitTransaction(new Transaction("cash deposit", null, myaccount2.getAccountNumber(), 1000d,
        TransactionType.CREDIT));
    bank.submitTransaction(new Transaction("cash deposit", myaccount1.getAccountNumber(), myaccount2.getAccountNumber(), 500d,
        TransactionType.TRANSFER));
    bank.close();

    Assert.assertThat(myaccount1.getBalance(), is(500d));
    Assert.assertThat(myaccount2.getBalance(), is(1500d));
    Assert.assertThat(bank.getLiquidity(), is(2000d));

  }


}
