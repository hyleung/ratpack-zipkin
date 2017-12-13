package ratpack.zipkin;

import ratpack.http.Status;
import ratpack.path.PathBinding;

import java.util.Optional;

/**
 * Interface for a wrapper around {@link ratpack.http.Response} that provides
 * some additional data that is used in the server parser.
 *
 */
public interface ServerResponse {
  /**
   * The path binding.
   *
   * @return Optional of PathBinding
   */
  Optional<PathBinding> pathBinding();

  /**
   * The original request.
   *
   * @return the server request
   */
  ServerRequest getRequest();

  /**
   * The HTTP status of the response.
   *
   * @return the HTTP status of the response
   */
  Status getStatus();
}
