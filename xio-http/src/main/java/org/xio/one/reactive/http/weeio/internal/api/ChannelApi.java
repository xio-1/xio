package org.xio.one.reactive.http.weeio.internal.api;


import org.apache.http.HttpStatus;
import org.xio.one.reactive.flow.domain.item.Item;
import org.xio.one.reactive.flow.subscribers.MultiItemSubscriber;
import org.xio.one.reactive.flow.subscribers.internal.Subscriber;
import org.xio.one.reactive.http.weeio.internal.domain.Event;
import org.xio.one.reactive.http.weeio.internal.service.EventChannel;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static javax.ws.rs.core.MediaType.SERVER_SENT_EVENTS;

@Path("/channel")
public class ChannelApi {

  private static Logger logger = Logger.getLogger(ChannelApi.class.getCanonicalName());
  private static SseBroadcaster sseBroadcaster;
  private static Subscriber sseSubscriber;

  @GET
  @Path("/{channelname}/_status")
  @Produces("application/json")
  public Response getInfo(@PathParam("channelname") String channelname) {
    try {
      return Response.status(HttpStatus.SC_ACCEPTED)
          .entity("{\"status\":" + EventChannel.channel(channelname).isAlive() + "}").build();
    } catch (Exception e) {
      throw new BadRequestException(e);
    }
  }

  @GET
  @POST
  @Path("/{channelname}/sse/enable")
  @Produces("application/json")
  public Response enableSSE(@PathParam("channelname") String channelname, @Context Sse sse) {
    if (this.sseSubscriber == null || this.sseSubscriber.isDone()) {
      final OutboundSseEvent.Builder builder = sse.newEventBuilder();
      this.sseSubscriber = EventChannel.channel(channelname+"_sink").flow()
          .addSubscriber(new MultiItemSubscriber<String, Event>() {

            @Override
            public void initialise() {
              if (getSseBroadcaster() == null)
                setSseBroadcaster(sse.newBroadcaster());
            }

            @Override
            public void onNext(Stream<Item<Event, String>> items) {
              items.forEach(i -> sseBroadcaster.broadcast(
                  builder.data(String.class, i.value().toJSONString())
                      .mediaType(MediaType.APPLICATION_JSON_TYPE).build()));
            }
          });
      logger
          .info("Started SSE subscriber " + sseSubscriber.getId() + " for channel " + channelname);
    }
    return Response.status(HttpStatus.SC_ACCEPTED).entity("{\"sse\":" + "\"enabled\"" + "}")
        .build();
  }

  @GET
  @Path("/{channelname}/sse/subscribe")
  @Produces(SERVER_SENT_EVENTS)
  public void get(@PathParam("channelname") String channelname, @Context SseEventSink eventSink) {
    this.sseBroadcaster.register(eventSink);
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
      logger.fine(event.toSSEFull());
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

  @GET
  @Path("/{channelname}/{querytype}")
  @Produces("application/json")
  public Object getQuery(@PathParam("channelname") String channelname,
      @PathParam("querytype") EventChannel.QueryType queryType) {
    return JSONUtil
        .toJSONString(EventChannel.channel(channelname).query(channelname, queryType, null));
  }

  public void setSseBroadcaster(SseBroadcaster sseBroadcaster) {
    this.sseBroadcaster = sseBroadcaster;
  }

  public SseBroadcaster getSseBroadcaster() {
    return sseBroadcaster;
  }
}
