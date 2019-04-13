package org.xio.one.test.examples.bank.domain;

import java.util.ArrayList;
import java.util.List;

public class TransactionLedger {
  private List<Transaction> transactionLedger = new ArrayList<>();

  public void add(Transaction transaction) {
    transactionLedger.add(transaction);
  }

  public List<Transaction> getAll() {
    return List.copyOf(transactionLedger);
  }
}
