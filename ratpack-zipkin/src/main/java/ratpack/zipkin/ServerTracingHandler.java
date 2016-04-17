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

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerRequestAdapter;
import com.github.kristofa.brave.ServerRequestInterceptor;
import com.github.kristofa.brave.ServerResponseInterceptor;
import com.github.kristofa.brave.http.SpanNameProvider;
import ratpack.func.Function;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.zipkin.internal.ServerRequestAdapterFactory;
import ratpack.zipkin.internal.ServerResponseAdapterFactory;

import javax.inject.Inject;
import java.util.Collection;

/**
 * {@link Handler} for ZipKin tracing.
 */
public class ServerTracingHandler implements Handler {
  private final ServerRequestInterceptor requestInterceptor;
  private final ServerResponseInterceptor responseInterceptor;
  private final ServerRequestAdapterFactory requestAdapterFactory;
  private final ServerResponseAdapterFactory responseAdapterFactory;
  private final SpanNameProvider spanNameProvider;
  private final RequestAnnotationExtractor requestAnnotationExtractor;
  private final ResponseAnnotationExtractor responseAnnotationExtractor;

  @Inject
  public ServerTracingHandler(final ServerRequestInterceptor requestInterceptor,
                              final ServerResponseInterceptor responseInterceptor,
                              final ServerRequestAdapterFactory requestAdapterFactory,
                              final ServerResponseAdapterFactory responseAdapterFactory,
                              final SpanNameProvider spanNameProvider,
                              final RequestAnnotationExtractor requestAnnotationExtractor,
                              final ResponseAnnotationExtractor responseAnnotationExtractor) {
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
    this.requestAdapterFactory = requestAdapterFactory;
    this.responseAdapterFactory = responseAdapterFactory;
    this.spanNameProvider = spanNameProvider;
    this.requestAnnotationExtractor = requestAnnotationExtractor;
    this.responseAnnotationExtractor = responseAnnotationExtractor;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    ServerRequestAdapter requestAdapter = requestAdapterFactory.createAdapter(spanNameProvider,
        ctx.getRequest(), requestAnnotationExtractor);
    requestInterceptor.handle(requestAdapter);
    ctx.getResponse()
       .beforeSend(response -> responseInterceptor
           .handle(responseAdapterFactory.createAdapter(response, responseAnnotationExtractor)));
    ctx.next();
  }
}