package org.xio.one.reactive.http.weeio.internal.api;


import org.apache.http.HttpStatus;
import org.xio.one.reactive.http.weeio.internal.domain.Event;
import org.xio.one.reactive.http.weeio.internal.domain.request.FilterExpression;
import org.xio.one.reactive.http.weeio.internal.domain.request.PassthroughExpression;
import org.xio.one.reactive.http.weeio.internal.domain.response.SubscriptionResponse;
import org.xio.one.reactive.http.weeio.internal.service.EventChannel;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.Sse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Logger;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Path("/channel")
public class ChannelApi {

  private static Logger logger = Logger.getLogger(ChannelApi.class.getCanonicalName());

  @GET
  @Path("/{channelname}")
  @Produces("application/json")
  public Response getInfo(@PathParam("channelname") String channelname) {
    try {
      return Response.status(HttpStatus.SC_ACCEPTED)
          .entity("{\"alive\":" + EventChannel.channel(channelname).isAlive() + "}").build();
    } catch (Exception e) {
      throw new BadRequestException(e);
    }
  }

  @PUT
  @POST
  @PATCH
  @Path("/{channelname}")
  @Consumes("application/json")
  public Response putOne(@Context HttpServletRequest request,
      @PathParam("channelname") String channelname, Event event) {
    try {
      HashMap<String, Enumeration<String>> headermap = new HashMap<>();
      request.getHeaderNames().asIterator()
          .forEachRemaining(h -> headermap.put(h, request.getHeaders(h)));
      if (headermap.containsKey("eventType"))
        event.addEventType(headermap.get("eventType").nextElement());
      else
        event.addEventType(channelname);
      event.addHTTPHeaders(headermap);
      event.addRequestMethod(request.getMethod());
      EventChannel.channel(channelname).flow().putItem(event);
      logger.info(event.toSSEFull());
      return Response.status(HttpStatus.SC_ACCEPTED).build();
    } catch (Exception e) {
      return Response.status(HttpStatus.SC_METHOD_FAILURE).build();
    }
  }

  @PUT
  @Path("/{channelname}/bulk")
  @Consumes("application/json")
  public Response putAll(@PathParam("channelname") String channelname, Event[] event) {
    try {
      EventChannel.channel(channelname).flow().putItem(event);
      return Response.status(HttpStatus.SC_ACCEPTED).build();
    } catch (Exception e) {
      return Response.status(HttpStatus.SC_METHOD_FAILURE).build();
    }
  }

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

  @GET
  @Path("/{channelname}/{querytype}")
  @Produces("application/json")
  public Object getQuery(@PathParam("channelname") String channelname,
      @PathParam("querytype") EventChannel.QueryType queryType) {
    return JSONUtil
        .toJSONString(EventChannel.channel(channelname).query(channelname, queryType, null));
  }

}
