package ratpack.zipkin.internal;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.TraceContext;
import javax.inject.Inject;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.zipkin.ServerTracingHandler;

/**
 * {@link Handler} for ZipKin tracing.
 */
public final class DefaultServerTracingHandler implements ServerTracingHandler {

  private final Tracing tracing;
  private final HttpServerHandler<Request, Response> handler;
  private final TraceContext.Extractor<Request> extractor;

  @Inject
  public DefaultServerTracingHandler(HttpTracing httpTracing) {
    this.tracing = httpTracing.tracing();
    this.handler = HttpServerHandler.create(httpTracing, new HttpAdapter());
    this.extractor = tracing.propagation().extractor((Request r, String name) -> r.getHeaders().get(name));
  }

  @Override
  public void handle(Context ctx) throws Exception {
    Request request = ctx.getRequest();
    final Span span = handler.handleReceive(extractor, request);

    ctx.getResponse().beforeSend(response -> {
      handler.handleSend(response, null, span);
      span.finish();
    });

    try(Tracer.SpanInScope ws = tracing.tracer().withSpanInScope(span)) {
      span.start();
      ctx.next();
    }
  }

  static final class HttpAdapter extends brave.http.HttpServerAdapter<Request, Response> {

    @Override public String method(Request request) {
      return request.getMethod().getName();
    }

    @Override public String path(Request request) {
      return request.getPath();
    }

    @Override public String url(Request request) {
      return request.getUri();
    }

    @Override public String requestHeader(Request request, String name) {
      return request.getHeaders().get(name);
    }

    @Override public Integer statusCode(Response response) {
      return response.getStatus().getCode();
    }
  }
}
