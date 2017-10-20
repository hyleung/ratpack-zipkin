package ratpack.zipkin;

import ratpack.path.PathBinding;

import java.util.Optional;

/**
 * Functional interface for span name customizers.
 */
@FunctionalInterface
public interface SpanNameProvider {
  /**
   * Given a request and response, return the name to be used for the Span.
   * @param request the request
   * @param pathBinding Optional of PathBinding. With RatPack, the PathBinding
   *                    is only available right before the response is sent.
   *                    This function is called *twice* - once when the server
   *                    request is *received* (with an empty pathBinding value)
   *                    and again before the response is *sent* (this time
   *                    the Optional will have a value).
   *
   * @return the Span name
   */
  String spanName(ServerRequest request, Optional<PathBinding> pathBinding);
}
