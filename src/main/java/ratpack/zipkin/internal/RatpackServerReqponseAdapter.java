package ratpack.zipkin.internal;

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerResponseAdapter;
import ratpack.http.Response;

import java.util.Collection;

/**
 * Created by hyleung on 2016-04-04.
 */
public class RatpackServerReqponseAdapter implements ServerResponseAdapter {
  private final Response response;

  public RatpackServerReqponseAdapter(final Response response) {
    this.response = response;
  }

  @Override
  public Collection<KeyValueAnnotation> responseAnnotations() {
    throw new RuntimeException("Not implemented!");
  }
}
