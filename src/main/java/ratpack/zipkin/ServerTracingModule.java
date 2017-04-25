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
import brave.sampler.Sampler;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Collections;
import ratpack.api.Nullable;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.http.client.HttpClient;
import ratpack.server.ServerConfig;
import ratpack.zipkin.internal.DefaultServerTracingHandler;
import ratpack.zipkin.internal.DefaultSpanNameProvider;
import ratpack.zipkin.internal.RatpackCurrentTraceContext;
import ratpack.zipkin.internal.ZipkinHttpClientImpl;
import zipkin.Endpoint;
import zipkin.Span;
import zipkin.reporter.Reporter;

/**
 * Module for ZipKin distributed tracing.
 */
public class ServerTracingModule extends ConfigurableModule<ServerTracingModule.Config> {
  @Override
  protected void configure() {
    bind(ServerTracingHandler.class).to(DefaultServerTracingHandler.class);
    Provider<ServerTracingHandler> serverTracingHandlerProviderProvider =
        getProvider(ServerTracingHandler.class);


    bind(HttpClient.class).annotatedWith(Zipkin.class).to(ZipkinHttpClientImpl.class);

    OptionalBinder.newOptionalBinder(binder(), SpanNameProvider.class)
        .setDefault().to(DefaultSpanNameProvider.class).in(Scopes.SINGLETON);

    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding()
        .toProvider(() -> HandlerDecorator.prepend(serverTracingHandlerProviderProvider.get()));
  }

  @Provides
  public Tracing getTracing(final Config config, final ServerConfig serverConfig) {
    return Tracing.newBuilder()
        .sampler(config.sampler)
        .currentTraceContext(new RatpackCurrentTraceContext())
        .localEndpoint(buildLocalEndpoint(config.serviceName, serverConfig.getPort(), serverConfig.getAddress()))
        .localServiceName(config.serviceName)
        .reporter(config.spanReporter)
        .build();
  }

  private static Endpoint buildLocalEndpoint(String serviceName, int port, @Nullable InetAddress configAddress) {
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

  private static InetAddress getSiteLocalAddress() {
    try {
      return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
              .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
              .filter(InetAddress::isSiteLocalAddress)
              .findAny().orElse(InetAddress.getLoopbackAddress());
    } catch (Exception e) {
      return InetAddress.getLoopbackAddress();
    }
  }


  /**
   * Configuration class for {@link ServerTracingModule}.
   */
  public static class Config {
    private String serviceName = "unknown";
    private Reporter<Span> spanReporter = Reporter.NOOP;
    private Sampler sampler = Sampler.NEVER_SAMPLE;

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

  }
}
