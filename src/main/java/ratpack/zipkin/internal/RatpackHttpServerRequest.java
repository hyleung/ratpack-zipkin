package ratpack.zipkin.internal;

import com.github.kristofa.brave.http.HttpServerRequest;
import ratpack.http.Request;

import java.net.URI;

/**
 * Created by hyleung on 2016-04-05.
 */
public class RatpackHttpServerRequest implements HttpServerRequest {
  private final Request request;

  public RatpackHttpServerRequest(final Request request) {
    this.request = request;
  }

  @Override
  public String getHttpHeaderValue(final String headerName) {
    throw new RuntimeException("Not implemented!");
  }

  @Override
  public URI getUri() {
    throw new RuntimeException("Not implemented!");
  }

  @Override
  public String getHttpMethod() {
    throw new RuntimeException("Not implemented!");
  }
}
