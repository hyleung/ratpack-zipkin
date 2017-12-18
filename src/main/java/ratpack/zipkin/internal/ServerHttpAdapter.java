package ratpack.zipkin.internal;

import com.google.common.net.HttpHeaders;
import ratpack.zipkin.ServerRequest;
import ratpack.zipkin.ServerResponse;
import zipkin2.Endpoint;

/**
 * This class is responsible for adapting Ratpack-specific request and responses
 * to something that brave.http.HttpServerParser can use to create the Span.
 */
final class ServerHttpAdapter extends brave.http.HttpServerAdapter<ServerRequest, ServerResponse> {
  @Override
  public boolean parseClientAddress(final ServerRequest serverRequest,
                                    final Endpoint.Builder builder) {
    String forwardedFor = requestHeader(serverRequest, HttpHeaders.X_FORWARDED_FOR);
    if (forwardedFor != null) {
      return builder.parseIp(forwardedFor);
    }
    return builder.parseIp(serverRequest.getRemoteAddress().getHostText());
  }

  @Override public String method(ServerRequest request) {
    return request.getMethod().getName();
  }

  @Override public String path(ServerRequest request) {
    return request.getPath();
  }

  @Override public String url(ServerRequest request) {
    return request.getUrl();
  }

  @Override public String requestHeader(ServerRequest request, String name) {
    return request.getHeaders().get(name);
  }

  @Override public Integer statusCode(ServerResponse response) {
    return response.getStatus().getCode();
  }
}
