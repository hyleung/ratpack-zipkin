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
package ratpack.zipkin.internal;

import com.github.kristofa.brave.ServerRequestAdapter;
import com.github.kristofa.brave.http.SpanNameProvider;
import ratpack.http.Request;
import ratpack.zipkin.RequestAnnotationExtractor;

import javax.inject.Inject;


/**
 * This class is responsible for constructing a {@link ServerRequestAdapter} instance.
 */
public class ServerRequestAdapterFactory {
  private final SpanNameProvider spanNameProvider;
  private final RequestAnnotationExtractor extractor;

  @Inject
  public ServerRequestAdapterFactory(final SpanNameProvider spanNameProvider,
                                     final RequestAnnotationExtractor extractor) {
    this.spanNameProvider = spanNameProvider;
    this.extractor = extractor;
  }

  /**
   * Create an adapter.
   *
   * @param request the request
   * @return a server request adapter
   */
  public ServerRequestAdapter createAdapter(final Request request) {
    return new RatpackServerRequestAdapter(spanNameProvider, request, extractor);
  }
}
