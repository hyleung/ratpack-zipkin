package ratpack.zipkin;

import ratpack.http.client.HttpResponse;
import ratpack.http.client.RequestSpec;

public interface ClientTracingInterceptor {
  void request(RequestSpec spec);

  void response(HttpResponse response);

  void error(Throwable e);
}
