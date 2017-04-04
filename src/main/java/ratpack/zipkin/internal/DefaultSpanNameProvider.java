package ratpack.zipkin.internal;

import ratpack.http.HttpMethod;
import ratpack.http.Request;
import ratpack.http.client.RequestSpec;
import ratpack.zipkin.SpanNameProvider;

public class DefaultSpanNameProvider implements SpanNameProvider {

  @Override
  public String getName(SpanNameProviderAdapter adapter) {
    return adapter.getMethod().getName();
  }

}
