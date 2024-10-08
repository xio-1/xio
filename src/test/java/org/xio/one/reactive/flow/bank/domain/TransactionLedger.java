package org.xio.one.reactive.flow.bank.domain;

import java.util.ArrayList;
import java.util.List;

public class TransactionLedger {

  private final List<AccountTransaction> transactionLedger = new ArrayList<>();

  public void add(AccountTransaction transaction) {
    transactionLedger.add(transaction);
  }

  public List<AccountTransaction> getAll() {
    return List.copyOf(transactionLedger);
  }
}
