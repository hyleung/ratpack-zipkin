package ratpack.zipkin;

import ratpack.func.Pair;
import java.util.List;

@FunctionalInterface
public interface RequestTagCustomizer {

  /**
   * Return the tags for a request.
   * @param request the request
   * @return a list of key/value pairs to add to the span as tags.
   */
  List<Pair<String, String>> tags(ServerRequest request);
}
