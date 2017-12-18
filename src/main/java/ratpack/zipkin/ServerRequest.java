package ratpack.zipkin;

import com.google.common.net.HostAndPort;
import ratpack.http.Headers;
import ratpack.http.HttpMethod;

/**
 * Read-only API for wrappers around Ratpack's {@link ratpack.http.Request}.
 */
public interface ServerRequest {
  /**
   * The method of the request.
   *
   * @return The method of the request.
   */
  HttpMethod getMethod();
  /**
   * The complete URI of the request (path + query string).
   *
   * @return The complete URI of the request (path + query string).
   */
  String getUri();

  /**
   * The URI without the query string and leading forward slash.
   *
   * @return The URI without the query string and leading forward slash
   */
  String getPath();

  /**
   * The request headers.
   *
   * @return The request headers.
   */
  Headers getHeaders();

  /**
   * The full URL of the request (scheme, host, port, etc.)
   * @return the full URL
   */
  String getUrl();

  /**
   * The address of the client making the request.
   * @return the host and port for the client
   */
  HostAndPort getRemoteAddress();
}
