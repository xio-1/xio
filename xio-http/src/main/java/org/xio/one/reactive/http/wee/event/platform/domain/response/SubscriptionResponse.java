package org.xio.one.reactive.http.wee.event.platform.domain.response;

public class SubscriptionResponse {
  private String clientID;

  public SubscriptionResponse(String clientID) {
    this.clientID = clientID;
  }

  public String getClientID() {
    return clientID;
  }
}
