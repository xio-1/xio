package org.xio.one.reactive.http.wee.event.platform.api;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class ApiBootstrap extends Application {

  private static UndertowJaxrsServer server;

  public static void main(String args[]) {
    Undertow.Builder serverBuilder =
        Undertow.builder().addHttpListener(8080, "0.0.0.0").setHandler(new HttpHandler() {
          @Override
          public void handleRequest(final HttpServerExchange exchange) throws Exception {
            try {
              exchange.dispatch(this);
            } catch (Exception e) {
              if (exchange.isResponseChannelAvailable()) {
                throw (new BadRequestException(e));
              }
            }
          }
        });

    server = new UndertowJaxrsServer().start(serverBuilder);
    DeploymentInfo di = server.undertowDeployment(ApiBootstrap.class);
    di.setContextPath("/");
    di.setDeploymentName("org.xio.one.reactive.http.wee");
    server.deploy(di);
  }

  @Override
  public Set<Class<?>> getClasses() {
    HashSet<Class<?>> classes = new HashSet<Class<?>>();
    classes.add(ChannelApi.class);
    return classes;
  }
}
