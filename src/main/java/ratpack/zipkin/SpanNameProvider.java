package ratpack.zipkin;

/**
 * Functional interface for span name customizers.
 */
@FunctionalInterface
public interface SpanNameProvider {
  /**
   * Given a request and response, return the name to be used for the Span.
   * @param request the request
   * @param response the response
   * @return the Span name
   */
  String spanName(ServerRequest request, ServerResponse response);
}
