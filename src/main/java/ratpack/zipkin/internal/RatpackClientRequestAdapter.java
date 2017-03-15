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
import com.github.kristofa.brave.http.SpanNameProvider;
import com.github.kristofa.brave.internal.Nullable;
import com.twitter.zipkin.gen.Endpoint;
import java.util.Arrays;
import ratpack.http.client.RequestSpec;

import java.util.Collection;
import java.util.Collections;
import zipkin.TraceKeys;

class RatpackClientRequestAdapter implements ClientRequestAdapter {
  private final RequestSpec requestSpec;
  private final SpanNameProvider spanNameProvider;
  private HttpClientRequest clientRequest;

  RatpackClientRequestAdapter(final RequestSpec requestSpec, final String method,
                              final SpanNameProvider spanNameProvider) {
    this.requestSpec = requestSpec;
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
          IdConversion.convertToString(spanId.traceId));
      requestSpec.getHeaders().add(BraveHttpHeaders.SpanId.getName(),
          IdConversion.convertToString(spanId.spanId));
      if (spanId.nullableParentId() != null) {
        requestSpec.getHeaders().add(BraveHttpHeaders.ParentSpanId.getName(),
            IdConversion.convertToString(spanId.parentId));
      }
    }
  }

  @Override
  public Collection<KeyValueAnnotation> requestAnnotations() {
    return Arrays.asList(
        KeyValueAnnotation.create(TraceKeys.HTTP_HOST, requestSpec.getUri().getHost()),
        KeyValueAnnotation.create(TraceKeys.HTTP_PATH, requestSpec.getUri().getPath())
    );
  }

  @Override
  public Endpoint serverAddress() {
    return null;
  }
}
