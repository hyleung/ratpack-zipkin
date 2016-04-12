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
package com.example.server;

import com.github.kristofa.brave.*;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.http.ServiceNameProvider;
import com.github.kristofa.brave.http.SpanNameProvider;
import com.github.kristofa.brave.http.StringServiceNameProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

class BraveModule extends AbstractModule {
  private final String serviceName;

  BraveModule(final String serviceName) {
    this.serviceName = serviceName;
  }

  @Provides
  public Brave brave() {
    Brave.Builder builder = new Brave
        .Builder(serviceName);
    return builder
        .build();
  }
  @Provides
  public ServerTracer getServerTracer(final Brave brave) {
    return brave.serverTracer();
  }

  @Provides
  public LocalTracer localTracer(final Brave brave) {
    return brave.localTracer();
  }

  @Provides
  public ServerResponseInterceptor serverResponseInterceptor(final ServerTracer tracer) {
    return new ServerResponseInterceptor(tracer);
  }

  @Provides
  public ServerRequestInterceptor serverRequestInterceptor(final ServerTracer tracer) {
    return new ServerRequestInterceptor(tracer);
  }

  @Provides
  public SpanNameProvider spanNameProvider() {
    return new DefaultSpanNameProvider();
  }

  @Provides
  public ServiceNameProvider serviceNameProvider() {
    return new StringServiceNameProvider("api-gateway");
  }

  @Override
  protected void configure() {

  }
}
