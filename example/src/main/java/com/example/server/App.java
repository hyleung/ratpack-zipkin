package com.example.server;

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.LoggingSpanCollector;
import com.github.kristofa.brave.Sampler;
import com.github.kristofa.brave.scribe.ScribeSpanCollector;
import com.google.common.collect.Lists;
import org.slf4j.MDC;
import ratpack.guice.Guice;
import ratpack.handling.RequestId;
import ratpack.logging.MDCInterceptor;
import ratpack.server.RatpackServer;
import ratpack.zipkin.ServerTracingModule;

/**
 * RatPack Server.
 */
public class App {
  public static void main(String[] args) throws Exception {
    Integer serverPort = Integer.parseInt(System.getProperty("port", "8081"));
    String scribeHost = System.getProperty("scribeHost");
    Float samplingPct = Float.parseFloat(System.getProperty("samplingPct","1"));
    RatpackServer.start(server -> server
        .serverConfig(config -> config.port(serverPort))
        .registry(Guice.registry(binding -> binding
            .module(ServerTracingModule.class, config -> config
                .serviceName("ratpack-demo")
                .sampler(Sampler.create(samplingPct))
                .spanCollector(scribeHost != null ? new ScribeSpanCollector(scribeHost, 9410) : new LoggingSpanCollector())
                .requestAnnotations(request ->
                    Lists.newArrayList(KeyValueAnnotation.create("uri", request.getUri()))
                )
                .responseAnnotations(response ->
                    Lists.newArrayList(KeyValueAnnotation.create("foo", "bar"))
                ))
            .bind(HelloWorldHandler.class)
            .add(MDCInterceptor.instance())
        ))
        .handlers(chain -> chain
            .get("hello", HelloWorldHandler.class)
            .all(ctx -> ctx.render("root")))
    );

    RatpackServer.start(server -> server
        .serverConfig(config -> config.port(serverPort + 1))
        .registry(Guice.registry(binding -> binding
            .module(ServerTracingModule.class, config -> config
                .serviceName("other-server")
                .sampler(Sampler.create(samplingPct))
                .spanCollector(scribeHost != null ? new ScribeSpanCollector(scribeHost, 9410) : new LoggingSpanCollector()))
            .bind(HelloWorldHandler.class)
            .add(MDCInterceptor.instance())
        ))
        .handlers(chain -> chain
            .all(ctx -> ctx.render("root")))
    );
  }

}
