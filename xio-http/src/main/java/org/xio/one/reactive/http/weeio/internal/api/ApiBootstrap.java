package org.xio.one.reactive.http.weeio.internal.api;

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

  public static void main(String args[]) throws Exception {
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
    start("0.0.0.0", 8080);
  }

  public static UndertowJaxrsServer start(String serverHostIPAddress, int port)
      throws Exception {
    Undertow.Builder serverBuilder =
        Undertow.builder().addHttpListener(port, serverHostIPAddress).setWorkerThreads(4);
    UndertowJaxrsServer server = new UndertowJaxrsServer().start(serverBuilder);
    DeploymentInfo di = server.undertowDeployment(ApiBootstrap.class);
    di.setContextPath("/");
    di.setDeploymentName("org.xio.one.reactive.http.weeio");
    server.deploy(di);
    return server;
  }

  @Override
  public Set<Class<?>> getClasses() {
    HashSet<Class<?>> classes = new HashSet<Class<?>>();
    classes.add(ChannelApi.class);
    return classes;
  }
}
