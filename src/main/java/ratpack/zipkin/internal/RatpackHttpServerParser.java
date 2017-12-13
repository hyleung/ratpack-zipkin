package ratpack.zipkin.internal;

import brave.SpanCustomizer;
import brave.http.HttpAdapter;
import brave.http.HttpServerParser;
import ratpack.zipkin.ServerRequest;
import ratpack.zipkin.ServerResponse;
import ratpack.zipkin.SpanNameProvider;

import java.util.Optional;

public class RatpackHttpServerParser extends HttpServerParser {
  private final SpanNameProvider spanNameProvider;

  public RatpackHttpServerParser(final SpanNameProvider spanNameProvider) {
    this.spanNameProvider = spanNameProvider;
  }
  @Override
  public <Req> void request(final HttpAdapter<Req, ?> adapter, final Req req, final SpanCustomizer customizer) {
    super.request(adapter, req, customizer);
  }

  @Override
  protected <Req> String spanName(final HttpAdapter<Req, ?> adapter, final Req req) {
    if (req instanceof ServerRequest) {
      return spanNameProvider.spanName((ServerRequest)req, Optional.empty());
    }
    else {
      return super.spanName(adapter, req);
    }
  }

  @Override
  public <Resp> void response(final HttpAdapter<?, Resp> adapter, final Resp res, final Throwable error, final SpanCustomizer customizer) {
    if (res instanceof ServerResponse) {
      ServerResponse serverResponse = (ServerResponse) res;
      customizer.name(spanNameProvider.spanName(serverResponse.getRequest(), serverResponse.pathBinding()));
    }
    super.response(adapter, res, error, customizer);
  }

  @Override
  protected void error(final Integer httpStatus, final Throwable error, final SpanCustomizer
      customizer) {
    super.error(httpStatus, error, customizer);
  }
}
