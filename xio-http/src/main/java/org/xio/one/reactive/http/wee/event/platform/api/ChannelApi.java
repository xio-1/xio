package org.xio.one.reactive.http.wee.event.platform.api;


import org.apache.http.HttpStatus;
import org.xio.one.reactive.http.wee.event.platform.domain.request.FilterExpression;
import org.xio.one.reactive.http.wee.event.platform.service.EventChannel;
import org.xio.one.reactive.http.wee.event.platform.domain.Event;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/channel")
public class ChannelApi {

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
  @Path("/{channelname}")
  @Consumes("application/json")
  public Response putOne(@PathParam("channelname") String channelname, Event event) {
    try {
      EventChannel.channel(channelname).flow().putItem(event);
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
  @Consumes("application/json")
  public Response subscribe(@PathParam("channelname") String channelname, FilterExpression filterEntryExpression) {
    try {
    } catch (Exception e) {
      return Response.status(HttpStatus.SC_METHOD_FAILURE).build();
    }
    return null;
  }

  @GET
  @Path("/{channelname}/{querytype}")
  @Produces("application/json")
  public Object getAll(@PathParam("channelname") String channelname,
      @PathParam("querytype") EventChannel.QueryType queryType)  {
    return JSONUtil.toJSONString(EventChannel.channel(channelname).query(channelname, queryType, null));
  }



}
