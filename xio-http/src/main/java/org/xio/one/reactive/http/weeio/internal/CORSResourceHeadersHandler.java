package org.xio.one.reactive.http.weeio.internal;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.HttpString;

public class CORSResourceHeadersHandler extends ResourceHandler {

  public CORSResourceHeadersHandler(ResourceManager resourceSupplier) {
    super(resourceSupplier);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    exchange.getResponseHeaders()
        .add(HttpString.tryFromString("Access-Control-Allow-Origin"), "*");

    exchange.getResponseHeaders().add(HttpString.tryFromString(
        "Access-Control-Allow-Credentials"), "true");

    exchange.getResponseHeaders().add(HttpString.tryFromString(
        "Access-Control-Allow-Headers"),
        "origin, content-type, accept, authorization");
    exchange.getResponseHeaders().add(HttpString.tryFromString(
        "Access-Control-Allow-Methods"),
        "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    super.handleRequest(exchange);
  }
}
