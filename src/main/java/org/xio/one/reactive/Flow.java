package org.xio.one.reactive;

import org.xio.one.reactive.flow.AsyncFlow;

public final class Flow {
  // input parameters
  private String streamName;
  private String indexFieldName;

  private Flow() {
  }

  public static Flow anAsyncFlow() {
    return new Flow();
  }

  public Flow withStreamName(String streamName) {
    this.streamName = streamName;
    return this;
  }

  public Flow withIndexFieldName(String indexFieldName) {
    this.indexFieldName = indexFieldName;
    return this;
  }

  public AsyncFlow build() {
    return new AsyncFlow(streamName, indexFieldName);
  }
}
