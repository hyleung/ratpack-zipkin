package ratpack.zipkin.internal;

import com.github.kristofa.brave.*;
import com.github.kristofa.brave.http.BraveHttpHeaders;
import com.github.kristofa.brave.http.HttpServerRequest;
import com.github.kristofa.brave.http.SpanNameProvider;

import java.util.Collection;

/**
 * Created by hyleung on 2016-04-04.
 */
public class RatpackServerRequestAdapter implements ServerRequestAdapter {
  private final SpanNameProvider spanNameProvider;
  private final HttpServerRequest request;

  public RatpackServerRequestAdapter(final SpanNameProvider spanNameProvider,
                                     final HttpServerRequest request) {
    this.spanNameProvider = spanNameProvider;
    this.request = request;
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
    throw new RuntimeException("Not implemented!");
  }

  private SpanId getSpanId(String traceId, String spanId, String parentSpanId) {
    if (parentSpanId != null)
    {
      return SpanId.create(IdConversion.convertToLong(traceId), IdConversion.convertToLong(spanId), IdConversion.convertToLong(parentSpanId));
    }
    return SpanId.create(IdConversion.convertToLong(traceId), IdConversion.convertToLong(spanId), null);
  }
}
