/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ratpack.zipkin;

import brave.Tracing;
import brave.http.HttpClientParser;
import brave.http.HttpServerParser;
import brave.http.HttpTracing;
import brave.internal.Platform;
import brave.sampler.Sampler;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import java.net.InetAddress;
import ratpack.api.Nullable;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.http.client.HttpClient;
import ratpack.server.ServerConfig;
import ratpack.zipkin.internal.DefaultServerTracingHandler;
import ratpack.zipkin.internal.RatpackCurrentTraceContext;
import ratpack.zipkin.internal.ZipkinHttpClientImpl;
import zipkin.Endpoint;
import zipkin.Span;
import zipkin.reporter.Reporter;

/**
 * Module for Zipkin distributed tracing.
 */
public class ServerTracingModule extends ConfigurableModule<ServerTracingModule.Config> {
  @Override
  protected void configure() {
    bind(ServerTracingHandler.class).to(DefaultServerTracingHandler.class);
    Provider<ServerTracingHandler> serverTracingHandlerProviderProvider =
        getProvider(ServerTracingHandler.class);


    bind(HttpClient.class).annotatedWith(Zipkin.class).to(ZipkinHttpClientImpl.class);

    OptionalBinder.newOptionalBinder(binder(), HttpClientParser.class)
        .setDefault().to(HttpClientParser.class).in(Scopes.SINGLETON);
    OptionalBinder.newOptionalBinder(binder(), HttpServerParser.class)
        .setDefault().to(HttpServerParser.class).in(Scopes.SINGLETON);

    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding()
        .toProvider(() -> HandlerDecorator.prepend(serverTracingHandlerProviderProvider.get()));
  }

  @Provides
  public HttpTracing getTracing(final Config config, final ServerConfig serverConfig) {
    Tracing tracing = Tracing.newBuilder()
        .sampler(config.sampler)
        .currentTraceContext(new RatpackCurrentTraceContext())
        .localEndpoint(buildLocalEndpoint(config.serviceName, serverConfig.getPort(),
            serverConfig.getAddress()))
        .localServiceName(config.serviceName)
        .reporter(config.spanReporter)
        .build();
    return HttpTracing.newBuilder(tracing)
        .clientParser(config.clientParser)
        .serverParser(config.serverParser).build();
  }

  private static Endpoint buildLocalEndpoint(String serviceName, int port, @Nullable InetAddress configAddress) {
    Endpoint.Builder builder = Endpoint.builder();
    if (!builder.parseIp(configAddress)) {
      builder = Platform.get().localEndpoint().toBuilder();
    }
    return builder.serviceName(serviceName).port(port).build();
  }

  /**
   * Configuration class for {@link ServerTracingModule}.
   */
  public static class Config {
    private String serviceName = "unknown";
    private Reporter<Span> spanReporter = Reporter.NOOP;
    private Sampler sampler = Sampler.NEVER_SAMPLE;
    private HttpClientParser clientParser = new HttpClientParser();
    private HttpServerParser serverParser = new HttpServerParser();

    public Config serviceName(final String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Config spanReporter(final Reporter<Span> reporter) {
      this.spanReporter = reporter;
      return this;
    }

    public Config sampler(final Sampler sampler) {
      this.sampler = sampler;
      return this;
    }

    public Config clientParser(final HttpClientParser clientParser) {
      this.clientParser = clientParser;
      return this;
    }

    public Config serverParser(final HttpServerParser serverParser) {
      this.serverParser = serverParser;
      return this;
    }
  }
}
