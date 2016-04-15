package com.example.server;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.google.common.collect.Lists;
import ratpack.server.RatpackServer;
import ratpack.zipkin.ServerTracingModule;

import static ratpack.guice.Guice.registry;

/**
 * RatPack Server.
 */
public class App {
  public static void main(String[] args) throws Exception {
    Integer serverPort = Integer.parseInt(System.getProperty("port", "8080"));
    RatpackServer.start(server ->
        server
            .serverConfig(config -> config.port(serverPort))
            .registry(registry(binding ->
                binding
                    .module(ServerTracingModule.class)
                    .bind(HelloWorldHandler.class)
                    .bindInstance(ServerTracingModule
                        .config()
                        .withBrave(new Brave.Builder("ratpack-demo")
                            .build())
                        .withRequestAnnotations(request ->
                            Lists.newArrayList(KeyValueAnnotation.create("uri", request.getUri()))
                        ))
            ))
            .handlers(handler ->
                handler
                    .get("hello", HelloWorldHandler.class)
                    .all(ctx -> ctx.render("root")))
    ).start();
  }

}
