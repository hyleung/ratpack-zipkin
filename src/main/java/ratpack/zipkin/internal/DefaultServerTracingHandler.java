package ratpack.zipkin.internal;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.TraceContext;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Headers;
import ratpack.http.HttpMethod;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.path.PathBinding;
import ratpack.zipkin.ServerRequest;
import ratpack.zipkin.ServerTracingHandler;
import ratpack.zipkin.SpanNameProvider;

import java.util.Optional;

/**
 * {@link Handler} for Zipkin tracing.
 */
public final class DefaultServerTracingHandler implements ServerTracingHandler {

  private final Tracing tracing;
  private final HttpServerHandler<ServerRequest, Response> handler;
  private final TraceContext.Extractor<ServerRequest> extractor;
  private final SpanNameProvider spanNameProvider;
  private final Logger logger = LoggerFactory.getLogger(DefaultServerTracingHandler.class);
  @Inject
  public DefaultServerTracingHandler(final HttpTracing httpTracing, final SpanNameProvider spanNameProvider) {
    this.tracing = httpTracing.tracing();
    this.handler = HttpServerHandler.<ServerRequest, Response>create(httpTracing, new ServerHttpAdapter());
    this.extractor = tracing.propagation().extractor((ServerRequest r, String name) -> r.getHeaders().get(name));
    this.spanNameProvider = spanNameProvider;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    ServerRequest request = new ServerRequestImpl(ctx.getRequest());
    final Span span = handler.handleReceive(extractor, request);
    span.name(spanNameProvider.spanName(request, Optional.empty()));
    final Tracer.SpanInScope scope = tracing.tracer().withSpanInScope(span);

    ctx.getResponse().beforeSend(response -> {
      span.name(spanNameProvider.spanName(request, Optional.ofNullable(ctx.getPathBinding())));
      handler.handleSend(response, null, span);
      span.finish();
      scope.close();
    });
    span.start();
    ctx.next();
  }

  private static class ServerRequestImpl implements ServerRequest {
   private final Request request;

    private ServerRequestImpl(final Request request) {
      this.request = request;
    }

    @Override
    public HttpMethod getMethod() {
      return request.getMethod();
    }

    @Override
    public String getUri() {
      return request.getUri();
    }

    @Override
    public String getPath() {
      return request.getPath();
    }

    @Override
    public Headers getHeaders() {
      return request.getHeaders();
    }
  }

}
