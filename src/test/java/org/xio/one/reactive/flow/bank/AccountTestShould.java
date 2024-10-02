package org.xio.one.reactive.flow.bank;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.flow.bank.domain.Account;
import org.xio.one.reactive.flow.bank.domain.InsufficientFundsException;

public class AccountTestShould {

  @Test
  public void returnBalanceOfZeroOnNewAccount() {
    Account account = new Account("myaccount", "12345");
    Assert.assertThat(account.getBalance(), is(0d));
  }

  @Test
  public void returnBalanceOfOneHundredWhenOneHundredCreditedToNewAccount() {
    Account account = new Account("myaccount", "12345");
    account.creditBalance(100d);
    Assert.assertThat(account.getBalance(), is(100d));
  }

  @Test
  public void returnBalanceOfOneHundredWhenTwoHundredCreditedToAndOneHundredDebitedFromNewAccount()
      throws InsufficientFundsException {
    Account account = new Account("myaccount", "12345");
    account.creditBalance(200d);
    account.debitBalance(100d);
    Assert.assertThat(account.getBalance(), is(100d));
  }

  @Test(expected = InsufficientFundsException.class)
  public void throwAnInsufficientFundsExceptionWhenDebitMadeGreaterThanBalance()
      throws InsufficientFundsException {
    Account account = new Account("myaccount", "12345");
    account.creditBalance(99d);
    account.debitBalance(100d);
  }


}
