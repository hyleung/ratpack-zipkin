package ratpack.zipkin.internal;

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerRequestAdapter;
import com.github.kristofa.brave.TraceData;
import com.github.kristofa.brave.http.SpanNameProvider;
import ratpack.http.Request;

import java.util.Collection;

/**
 * Created by hyleung on 2016-04-04.
 */
public class RatpackServerRequestAdapter implements ServerRequestAdapter {
  private final SpanNameProvider spanNameProvider;
  private final Request request;

  public RatpackServerRequestAdapter(final SpanNameProvider spanNameProvider,
                                     final Request request) {
    this.spanNameProvider = spanNameProvider;
    this.request = request;
  }

  @Override
  public TraceData getTraceData() {
    throw new RuntimeException("Not implemented!");
  }

  @Override
  public String getSpanName() {
    throw new RuntimeException("Not implemented!");
  }

  @Override
  public Collection<KeyValueAnnotation> requestAnnotations() {
    throw new RuntimeException("Not implemented!");
  }
}
