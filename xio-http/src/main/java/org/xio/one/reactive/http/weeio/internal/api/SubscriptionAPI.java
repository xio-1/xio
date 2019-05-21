package org.xio.one.reactive.http.weeio.internal.api;

import org.xio.one.reactive.http.weeio.internal.domain.request.FilterExpression;
import org.xio.one.reactive.http.weeio.internal.domain.request.PassthroughExpression;
import org.xio.one.reactive.http.weeio.internal.domain.response.SubscriptionResponse;
import org.xio.one.reactive.http.weeio.internal.service.EventChannel;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Path("/")
public class SubscriptionAPI {

  @POST
  @Path("/{channelname}/subscribe")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response subscribe(@PathParam("channelname") String channelname,
      FilterExpression filterExpression) {
    if (filterExpression == null)
      filterExpression = new PassthroughExpression();
    return Response.status(200).entity(new SubscriptionResponse(
        EventChannel.channel(channelname).registerNewClient(filterExpression)))
        .type(APPLICATION_JSON_TYPE).build();
  }


}
