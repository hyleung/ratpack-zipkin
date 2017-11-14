package ratpack.zipkin;

import ratpack.func.Pair;
import ratpack.http.Response;
import ratpack.path.PathBinding;

import java.util.List;

@FunctionalInterface
public interface ResponseTagCustomizer {
  /**
   * Return the tags for a response and pathbinding.
   * @param response the response
   * @param pathBinding the path binding
   * @return a list of key/value pairs to add to the span as tags.
   */
  List<Pair<String, String>> tags(Response response, PathBinding pathBinding);
}
