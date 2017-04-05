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

import brave.Tracer;
import brave.sampler.Sampler;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ClientRequestInterceptor;
import com.github.kristofa.brave.ClientResponseInterceptor;
import com.github.kristofa.brave.LocalTracer;
import com.github.kristofa.brave.ServerRequestInterceptor;
import com.github.kristofa.brave.ServerResponseInterceptor;
import com.github.kristofa.brave.TracerAdapter;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.http.SpanNameProvider;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import zipkin.Endpoint;
import ratpack.api.Nullable;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.http.client.HttpClient;
import ratpack.server.ServerConfig;
import ratpack.zipkin.internal.*;
import zipkin.Span;
import zipkin.reporter.Reporter;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Collections;

import static com.google.inject.Scopes.SINGLETON;

/**
 * Module for ZipKin distributed tracing.
 */
public class ServerTracingModule extends ConfigurableModule<ServerTracingModule.Config> {
  @Override
  protected void configure() {
    bind(ServerRequestAdapterFactory.class).in(SINGLETON);
    bind(ServerResponseAdapterFactory.class).in(SINGLETON);
    bind(ServerTracingHandler.class).to(DefaultServerTracingHandler.class);
    Provider<ServerTracingHandler> serverTracingHandlerProviderProvider = getProvider(ServerTracingHandler.class);

    bind(ClientRequestAdapterFactory.class).in(SINGLETON);
    bind(ClientResponseAdapterFactory.class).in(SINGLETON);


    bind(HttpClient.class).annotatedWith(Zipkin.class).to(ZipkinHttpClientImpl.class);
    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().toProvider(() -> HandlerDecorator.prepend(serverTracingHandlerProviderProvider.get()));
  }

  @Provides
  public SpanNameProvider spanNameProvider(final Config config) {
    return config.spanNameProvider;
  }

  @Provides
  public RequestAnnotationExtractor requestAnnotationExtractorFunc(final Config config) {
    return config.requestAnnotationFunc;
  }

  @Provides
  public ResponseAnnotationExtractor responseAnnotationExtractorFunc(final Config config) {
    return config.responseAnnotationFunc;
  }

  @Provides
  public ServerResponseInterceptor serverResponseInterceptor(final Brave brave) {
    return new ServerResponseInterceptor(brave.serverTracer());
  }

  @Provides
  public ServerRequestInterceptor serverRequestInterceptor(final Brave brave) {
    return new ServerRequestInterceptor(brave.serverTracer());
  }

  @Provides
  public ClientRequestInterceptor clientRequestInterceptor(final Brave brave) {
    return new ClientRequestInterceptor(brave.clientTracer());
  }

  @Provides
  public ClientResponseInterceptor clientResponseInterceptor(final Brave brave) {
    return new ClientResponseInterceptor(brave.clientTracer());
  }

  @Provides
  public Brave getBrave(final Tracer tracer) {
    return TracerAdapter.newBrave(tracer);
  }

  @Provides
  public Tracer getTracer(final Config config, final ServerConfig serverConfig) {
    Tracer.Builder builder = Tracer.newBuilder();

    builder.localEndpoint(buildLocalEndpoint(
        config.serviceName,
        serverConfig.getPort(),
        serverConfig.getAddress()
    ));
    builder.currentTraceContext(new RatpackCurrentTraceContext());

    if (config.spanReporter != null) {
      builder.reporter(config.spanReporter);
    }

    if (config.sampler != null) {
      builder.sampler(config.sampler);
    }
    return builder.build();
  }

  Endpoint buildLocalEndpoint(String serviceName, int port, @Nullable InetAddress configAddress) {
    Endpoint.Builder builder = Endpoint.builder()
            .serviceName(serviceName)
            .port(port);
    InetAddress address = configAddress != null ? configAddress : getSiteLocalAddress();
    if (address.getAddress().length == 4) {
      builder.ipv4(ByteBuffer.wrap(address.getAddress()).getInt());
    } else if (address.getAddress().length == 16) {
      builder.ipv6(address.getAddress());
    }
    return builder.build();
  }

  InetAddress getSiteLocalAddress() {
    try {
      return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
              .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
              .filter(InetAddress::isSiteLocalAddress)
              .findAny().get();
    } catch (Exception e) {
      return InetAddress.getLoopbackAddress();
    }
  }

  @Provides
  public LocalTracer localTracer(final Brave brave) {
    return brave.localTracer();
  }

  /**
   * Configuration class for {@link ServerTracingModule}.
   */
  public static class Config {
    private String serviceName = "unknown";
    private Reporter<Span> spanReporter;
    private Sampler sampler;
    private SpanNameProvider spanNameProvider = new DefaultSpanNameProvider();
    private RequestAnnotationExtractor requestAnnotationFunc = RequestAnnotationExtractor.DEFAULT;
    private ResponseAnnotationExtractor responseAnnotationFunc = ResponseAnnotationExtractor.DEFAULT;

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
    public Config spanNameProvider(final SpanNameProvider spanNameProvider) {
      this.spanNameProvider = spanNameProvider;
      return this;
    }

    public Config requestAnnotations(final RequestAnnotationExtractor func) {
      this.requestAnnotationFunc = func;
      return this;
    }

    public Config responseAnnotations(final ResponseAnnotationExtractor func) {
      this.responseAnnotationFunc = func;
      return this;
    }

  }
}
