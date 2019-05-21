package org.xio.one.reactive.http.weeio.internal.api;

import io.undertow.Undertow;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.serverSentEvents;

@ApplicationPath("/")
public class SubscriptionApiBootstrap extends Application {

  public static UndertowJaxrsServer startSubscriptionAPI(String serverHostIPAddress, int port) throws Exception {
    Undertow.Builder serverBuilder =
        Undertow.builder().addHttpListener(port, serverHostIPAddress).setWorkerThreads(Runtime.getRuntime().availableProcessors() + 14);
    UndertowJaxrsServer server = new UndertowJaxrsServer().start(serverBuilder);
    DeploymentInfo di = server.undertowDeployment(SubscriptionApiBootstrap.class);
    di.setContextPath("/");
    di.setDeploymentName("org.xio.one.reactive.http.weeio");
    server.deploy(di);
    return server;
  }

  @Override
  public Set<Class<?>> getClasses() {
    HashSet<Class<?>> classes = new HashSet<Class<?>>();
    classes.add(SubscriptionAPI.class);
    classes.add(CorsFilter.class);
    return classes;
  }
}
