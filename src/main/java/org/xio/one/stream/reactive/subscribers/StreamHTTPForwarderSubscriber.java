package org.xio.one.stream.reactive.subscribers;

import org.xio.one.stream.event.Event;
import org.xio.one.stream.selector.FilterEntry;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

public class StreamHTTPForwarderSubscriber extends SelectorCollectorSubscriber<Event[]> {

  private URI remoteStreamBulkURI;
  private boolean stopForwarding;

  public StreamHTTPForwarderSubscriber(URI remoteStreamBulkURI) {
    super();
    this.remoteStreamBulkURI = remoteStreamBulkURI;
  }

  public StreamHTTPForwarderSubscriber(URI remoteStreamBulkURI, FilterEntry filterEntry) {
    super(filterEntry);
    this.remoteStreamBulkURI = remoteStreamBulkURI;
  }

  public void stopForwarding() {
    stopForwarding = true;
  }

  public void startForwarding() {
    stopForwarding = false;
    Client client = ClientBuilder.newClient();
    WebTarget webTarget = client.target(remoteStreamBulkURI);

    Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
    while (stopForwarding == false) {
      Event[] events = this.getNext();
      if (events != null && events.length > 0) {
        Response response =
            invocationBuilder.put(Entity.entity(events, MediaType.APPLICATION_JSON));
        int status = response.getStatus();
        response.close();
        if (status != Response.Status.ACCEPTED.getStatusCode()) {
          System.out.println("FORWARDING ERROR " + status);
          stopForwarding = true;
        }
      }
    }

  }

}
