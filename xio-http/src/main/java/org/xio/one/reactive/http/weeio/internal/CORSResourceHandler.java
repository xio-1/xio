package org.xio.one.reactive.http.weeio.internal;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.ResourceSupplier;
import io.undertow.util.HttpString;

public class CORSResourceHandler extends ResourceHandler {

  public CORSResourceHandler(ResourceManager resourceSupplier) {
    super(resourceSupplier);
  }

  public CORSResourceHandler(ResourceManager resourceManager, HttpHandler next) {
    super(resourceManager, next);
  }

  public CORSResourceHandler(ResourceSupplier resourceSupplier) {
    super(resourceSupplier);
  }

  public CORSResourceHandler(ResourceSupplier resourceManager, HttpHandler next) {
    super(resourceManager, next);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    exchange.getResponseHeaders()
        .add(HttpString.tryFromString("Access" + "-Control-Allow-Origin"), "*");
    super.handleRequest(exchange);
  }
}
