package ratpack.zipkin;

import ratpack.http.HttpMethod;

public interface SpanNameProvider {
  String getName(SpanNameProviderAdapter adapter);

  interface SpanNameProviderAdapter {
    String getUri();
    HttpMethod getMethod();
  }
}
