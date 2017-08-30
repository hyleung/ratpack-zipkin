package ratpack.zipkin;

import ratpack.http.Status;
import ratpack.path.PathBinding;

/**
 * Read-only API for wrappers around Ratpack's {@link ratpack.http.Response}.
 *
 * This API *add* an accessor for the {@link PathBinding}, which is *not*
 * currently accessible in {@link ratpack.http.Response}.
 */
public interface ServerResponse {
  /**
   * The status that will be part of the response when sent.
   *
   * @return The status that will be part of the response when sent
   */
  Status getStatus();

  /**
   * Get the path binding for the response.
   * @return the path binding.
   */
  PathBinding getPathBinding();
}
