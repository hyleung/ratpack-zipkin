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

import com.github.kristofa.brave.ClientRequestAdapter;
import com.github.kristofa.brave.IdConversion;
import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.SpanId;
import com.github.kristofa.brave.http.BraveHttpHeaders;
import com.github.kristofa.brave.http.HttpClientRequest;
import com.github.kristofa.brave.http.ServiceNameProvider;
import com.github.kristofa.brave.http.SpanNameProvider;
import com.github.kristofa.brave.internal.Nullable;
import ratpack.http.client.RequestSpec;

import java.util.Collection;
import java.util.Collections;

public class RatpackClientRequestAdapter implements ClientRequestAdapter {
  private final RequestSpec requestSpec;
  private final ServiceNameProvider serviceNameProvider;
  private final SpanNameProvider spanNameProvider;
  private HttpClientRequest clientRequest;

  public RatpackClientRequestAdapter(final RequestSpec requestSpec, final String method, final
  ServiceNameProvider serviceNameProvider, final SpanNameProvider spanNameProvider) {
    this.requestSpec = requestSpec;
    this.serviceNameProvider = serviceNameProvider;
    this.spanNameProvider = spanNameProvider;
    this.clientRequest = new RatpackHttpClientRequest(requestSpec, method);
  }

  @Override
  public String getSpanName() {
    return spanNameProvider.spanName(clientRequest);
  }

  @Override
  public void addSpanIdToRequest(@Nullable final SpanId spanId) {
    if (spanId == null) {
      requestSpec.getHeaders().add(BraveHttpHeaders.Sampled.getName(), "0");
    } else {
      requestSpec.getHeaders().add(BraveHttpHeaders.Sampled.getName(), "1");
      requestSpec.getHeaders().add(BraveHttpHeaders.TraceId.getName(),
          IdConversion.convertToString(spanId.getTraceId()));
      requestSpec.getHeaders().add(BraveHttpHeaders.SpanId.getName(),
          IdConversion.convertToString(spanId.getSpanId()));
      if (spanId.getParentSpanId() != null) {
        requestSpec.getHeaders().add(BraveHttpHeaders.ParentSpanId.getName(),
            IdConversion.convertToString(spanId.getParentSpanId()));
      }
    }
  }

  @Override
  public String getClientServiceName() {
    return serviceNameProvider.serviceName(clientRequest);
  }

  @Override
  public Collection<KeyValueAnnotation> requestAnnotations() {
    return Collections.singletonList(KeyValueAnnotation.create("http.uri", requestSpec.getUrl().toString()));
  }
}
