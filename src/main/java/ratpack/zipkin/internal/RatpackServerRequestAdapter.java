/*
 * Copyright 2015 the original author or authors.
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

import com.github.kristofa.brave.*;
import com.github.kristofa.brave.http.BraveHttpHeaders;
import com.github.kristofa.brave.http.HttpServerRequest;
import com.github.kristofa.brave.http.SpanNameProvider;
import ratpack.http.Request;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of {@link ServerRequestAdapter} for RatPack.
 */
public class RatpackServerRequestAdapter implements ServerRequestAdapter {
  private final SpanNameProvider spanNameProvider;
  private final HttpServerRequest request;

  public RatpackServerRequestAdapter(final SpanNameProvider spanNameProvider,
                                     final Request request) {
    this.spanNameProvider = spanNameProvider;
    this.request = new RatpackHttpServerRequest(request);
  }

  @Override
  public TraceData getTraceData() {
    final String sampled = request.getHttpHeaderValue(BraveHttpHeaders.Sampled.getName());
    TraceData.Builder builder = TraceData.builder();
    if ("0".equals(sampled) || "false".equals(sampled)) {
      builder.sample(false);
    } else {
      final String parentSpanId = request.getHttpHeaderValue(BraveHttpHeaders.ParentSpanId.getName());
      final String traceId = request.getHttpHeaderValue(BraveHttpHeaders.TraceId.getName());
      final String spanId = request.getHttpHeaderValue(BraveHttpHeaders.SpanId.getName());
      if (traceId != null && spanId != null) {
        SpanId span = getSpanId(traceId, spanId, parentSpanId);
        return TraceData.builder()
                        .sample(true)
                        .spanId(span)
                        .build();
      }
    }
    return builder.build();
  }

  @Override
  public String getSpanName() {
    return spanNameProvider.spanName(request);
  }

  @Override
  public Collection<KeyValueAnnotation> requestAnnotations() {
    return Collections.emptyList();
  }

  private SpanId getSpanId(String traceId, String spanId, String parentSpanId) {
    if (parentSpanId != null)
    {
      return SpanId.create(IdConversion.convertToLong(traceId), IdConversion.convertToLong(spanId), IdConversion.convertToLong(parentSpanId));
    }
    return SpanId.create(IdConversion.convertToLong(traceId), IdConversion.convertToLong(spanId), null);
  }
}
