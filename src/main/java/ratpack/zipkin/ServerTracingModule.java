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

import brave.CurrentSpanCustomizer;
import brave.SpanCustomizer;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpClientParser;
import brave.http.HttpSampler;
import brave.http.HttpServerParser;
import brave.http.HttpTracing;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.http.client.HttpClient;
import ratpack.server.ServerConfig;
import ratpack.util.Exceptions;
import ratpack.zipkin.internal.DefaultClientTracingInterceptor;
import ratpack.zipkin.internal.DefaultServerTracingHandler;
import ratpack.zipkin.internal.RatpackCurrentTraceContext;
import ratpack.zipkin.internal.RatpackHttpServerParser;
import zipkin2.Span;
import zipkin2.reporter.Reporter;

/**
 * Module for Zipkin distributed tracing.
 */
public class ServerTracingModule extends ConfigurableModule<ServerTracingModule.Config> {

  @Override
  protected void configure() {
    bind(ServerTracingHandler.class)
        .to(DefaultServerTracingHandler.class)
        .in(Singleton.class);

    bind(ClientTracingInterceptor.class)
            .to(DefaultClientTracingInterceptor.class)
            .in(Singleton.class);

    Provider<ClientTracingInterceptor> clientTracingInterceptorProvider =
            getProvider(ClientTracingInterceptor.class);

    Provider<HttpClient> httpClientProvider = () ->
            // HttpClient.of throws exceptions but Providers can not, hence the unchecked wrapper.
            Exceptions.uncheck(() -> HttpClient.of(spec -> {
              spec.requestIntercept(clientTracingInterceptorProvider.get()::request);
              spec.responseIntercept(clientTracingInterceptorProvider.get()::response);
            }));

    bind(HttpClient.class)
            .toProvider(httpClientProvider)
            .in(Singleton.class);

    bind(HttpClient.class)
            .annotatedWith(Zipkin.class)
            .toProvider(httpClientProvider)
            .in(Singleton.class);

    Provider<ServerTracingHandler> serverTracingHandlerProvider =
        getProvider(ServerTracingHandler.class);

    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding()
        .toProvider(() -> HandlerDecorator.prepend(serverTracingHandlerProvider.get()))
        .in(Singleton.class);
  }

  @Provides @Singleton
  public SpanCustomizer getSpanCustomizer(final Tracing tracing) {
    return CurrentSpanCustomizer.create(tracing);
  }

  @Provides @Singleton
  public Tracer getTracer(final Tracing tracing) {
    return tracing.tracer();
  }

  @Provides @Singleton
  public Tracing getTracing(final HttpTracing httpTracing) {
    return httpTracing.tracing();
  }

  @Provides @Singleton
  public HttpTracing getHttpTracing(final Config config, final ServerConfig serverConfig) {
    Tracing.Builder builder = Tracing.newBuilder()
                             .sampler(config.sampler)
                             .currentTraceContext(new RatpackCurrentTraceContext())
                             .localServiceName(config.serviceName)
                             .localPort(serverConfig.getPort())
                             .spanReporter(config.spanReporter)
                             .propagationFactory(config.propagationFactory);
    if (serverConfig.getAddress() != null) {
        builder = builder.localIp(serverConfig.getAddress().getHostAddress());
    }

    return HttpTracing.newBuilder(builder.build())
                      .clientParser(config.clientParser)
                      .serverParser(config.serverParser)
                      .serverSampler(config.serverSampler)
                      .clientSampler(config.clientSampler)
                      .build();
  }

  /**
   * Configuration class for {@link ServerTracingModule}.
   */
  public static class Config {
    private String serviceName = "unknown";
    private Reporter<Span> spanReporter = Reporter.NOOP;
    private Sampler sampler = Sampler.NEVER_SAMPLE;
    private HttpSampler serverSampler = HttpSampler.TRACE_ID;
    private HttpSampler clientSampler = HttpSampler.TRACE_ID;

    private HttpClientParser clientParser = new HttpClientParser();
    private HttpServerParser serverParser = new HttpServerParser();
    private Propagation.Factory propagationFactory = B3Propagation.FACTORY;

    /**
     * Set the service name.
     *
     * @param serviceName the service name.
     * @return the config
     */
    public Config serviceName(final String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    /**
     * Set the Span reporter.
     *
     * If not set, defaults to {@link Reporter#NOOP}.
     *
     * @param reporter a V2 reporter
     *
     * @return the config
     */
    public Config spanReporterV2(final Reporter<Span> reporter) {
      this.spanReporter = reporter;
      return this;
    }

    /**
     * Set the sampler.
     *
     * If not set, defaults to {@link Sampler#NEVER_SAMPLE}.
     *
     * @param sampler the sampler
     *
     * @return the config
     */
    public Config sampler(final Sampler sampler) {
      this.sampler = sampler;
      return this;
    }

    /**
     * Set the {@link HttpSampler} for client requests.
     *
     * If not set, defaults to {@link HttpSampler#TRACE_ID}.
     *
     * @param clientSampler the client sampler
     *
     * @return the config
     */
    public Config clientSampler(final HttpSampler clientSampler) {
      this.clientSampler = clientSampler;
      return this;
    }

    /**
     * Set the {@link HttpSampler} for server requests.
     *
     * If not set, defaults to {@link HttpSampler#TRACE_ID}.
     *
     * @param httpSampler the server sampler
     *
     * @return the config
     */
    public Config serverSampler(final HttpSampler httpSampler) {
      this.serverSampler = httpSampler;
      return this;
    }

    /**
     * Set the {@link HttpClientParser}.
     *
     * Defaults to {@link HttpClientParser}, which implements some reasonable
     * defaults for client spans.
     * Provide a subclass of {@link HttpClientParser} to customize behaviour.
     *
     * @param clientParser the client parser
     *
     * @return the config
     */
    public Config clientParser(final HttpClientParser clientParser) {
      this.clientParser = clientParser;
      return this;
    }

    /**
     * Set the {@link HttpServerParser}.
     *
     * Defaults to {@link HttpServerParser}, which implements some reasonable
     * defaults for server spans. If set, this will override any SpanNameProvider.
     * Provide a subclass of {@link HttpServerParser} to customize behaviour.
     *
     * @param serverParser the server parser
     *
     * @return the config
     */
    public Config serverParser(final HttpServerParser serverParser) {
      this.serverParser = serverParser;
      return this;
    }

    /**
     * Set a function for customizing the Span name.
     *
     * @param spanNameProvider a function taking a request and response
     * @return the Span name
     */
    public Config spanNameProvider(final SpanNameProvider spanNameProvider) {
      this.serverParser = new RatpackHttpServerParser(spanNameProvider);
      return this;
    }

    /**
     * Set the {@link Propagation.Factory}.
     *
     * Defaults to {@link B3Propagation.Factory}.
     *
     * @param propagationFactory the Zipkin propagation factory
     * @return the config
     */
    public Config propagationFactory(final Propagation.Factory propagationFactory) {
      this.propagationFactory = propagationFactory;
      return this;
    }
  }
}
