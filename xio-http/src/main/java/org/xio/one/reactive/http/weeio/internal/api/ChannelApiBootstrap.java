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
public class ChannelApiBootstrap extends Application {

  private static final ServerSentEventHandler sseHandler = serverSentEvents();

  public static UndertowJaxrsServer startChannelAPI(String serverHostIPAddress, int port) throws Exception {
    Undertow.Builder serverBuilder =
        Undertow.builder().addHttpListener(port, serverHostIPAddress).setWorkerThreads(Runtime.getRuntime().availableProcessors() + 1).setHandler(path()
            .addPrefixPath("/sse", sseHandler));
    UndertowJaxrsServer server = new UndertowJaxrsServer().start(serverBuilder);
    DeploymentInfo di = server.undertowDeployment(ChannelApiBootstrap.class);
    di.setContextPath("/");
    di.setDeploymentName("org.xio.one.reactive.http.weeio");
    server.deploy(di);
    return server;
  }

  public static ServerSentEventHandler getSseHandler() {
    return sseHandler;
  }

  @Override
  public Set<Class<?>> getClasses() {
    HashSet<Class<?>> classes = new HashSet<Class<?>>();
    classes.add(ChannelApi.class);
    classes.add(CorsFilter.class);
    classes.add(ServerSentEventHandler.class);
    return classes;
  }
}
