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
import com.github.kristofa.brave.http.SpanNameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.http.Request;
import ratpack.zipkin.RequestAnnotationExtractor;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of {@link ServerRequestAdapter} for RatPack.
 */
class RatpackServerRequestAdapter implements ServerRequestAdapter {
  private final Logger logger = LoggerFactory.getLogger(RatpackServerRequestAdapter.class);
  private final SpanNameProvider spanNameProvider;
  private final Request request;
  private final RequestAnnotationExtractor annotationExtractor;

  RatpackServerRequestAdapter(final SpanNameProvider spanNameProvider,
                              final Request request,
                              final RequestAnnotationExtractor annotationExtractor) {
    this.spanNameProvider = spanNameProvider;
    this.request = request;
    this.annotationExtractor = annotationExtractor;
  }

  @Override
  public TraceData getTraceData() {

    final String sampled = request.getHeaders().get(BraveHttpHeaders.Sampled.getName());
    TraceData.Builder builder = TraceData.builder();
    if ("0".equals(sampled) || "false".equals(sampled)) {
      builder.sample(false);
    } else {
      final String parentSpanId = request.getHeaders().get(BraveHttpHeaders.ParentSpanId.getName());
      final String traceId = request.getHeaders().get(BraveHttpHeaders.TraceId.getName());
      final String spanId = request.getHeaders().get(BraveHttpHeaders.SpanId.getName());
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
    return spanNameProvider.spanName(new RatpackHttpServerRequest(request));
  }

  @Override
  public Collection<KeyValueAnnotation> requestAnnotations() {
    try {
      Collection<KeyValueAnnotation> annotations = annotationExtractor
          .annotationsForRequest(request);
      logger.debug("Request annotations: {}", annotations);
      return annotations;
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  private SpanId getSpanId(String traceId, String spanId, String parentSpanId) {
    if (parentSpanId != null)
    {
      return SpanId.create(IdConversion.convertToLong(traceId), IdConversion.convertToLong(spanId), IdConversion.convertToLong(parentSpanId));
    }
    return SpanId.create(IdConversion.convertToLong(traceId), IdConversion.convertToLong(spanId), null);
  }
}
