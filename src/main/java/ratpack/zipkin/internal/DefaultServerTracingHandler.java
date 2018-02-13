package ratpack.zipkin.internal;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.TraceContext;
import javax.inject.Inject;

import com.google.common.net.HostAndPort;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Headers;
import ratpack.http.HttpMethod;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.path.PathBinding;
import ratpack.server.PublicAddress;
import ratpack.zipkin.ServerRequest;
import ratpack.zipkin.ServerResponse;
import ratpack.zipkin.ServerTracingHandler;

import java.util.Optional;

/**
 * {@link Handler} for Zipkin tracing.
 */
public final class DefaultServerTracingHandler implements ServerTracingHandler {

  private final Tracing tracing;
  private final HttpServerHandler<ServerRequest, ServerResponse> handler;
  private final TraceContext.Extractor<ServerRequest> extractor;

  @Inject
  public DefaultServerTracingHandler(final HttpTracing httpTracing) {
    this.tracing = httpTracing.tracing();
    this.handler = HttpServerHandler.<ServerRequest, ServerResponse>create(httpTracing, new ServerHttpAdapter());
    this.extractor = tracing.propagation().extractor((ServerRequest r, String name) -> r.getHeaders().get(name));
  }

  @Override
  public void handle(Context ctx) throws Exception {
    ServerRequest request = new ServerRequestImpl(ctx.getRequest());
    final Span span = handler.handleReceive(extractor, request);

    ctx.getResponse().beforeSend(response -> {
      ServerResponse serverResponse = new ServerResponseImpl(response, request, ctx.getPathBinding());
      handler.handleSend(serverResponse, null, span);
    });
    //place the Span in scope so that downstream code (e.g. Ratpack handlers
    //further on in the chain) can see the Span.
    try (Tracer.SpanInScope scope = tracing.tracer().withSpanInScope(span)) {
      ctx.next();
    }
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

    @Override
    public String getUrl() {
      PublicAddress publicAddress = request.get(Context.class).get(PublicAddress.class);
      return publicAddress.builder()
                          .path(request.getPath())
                          .params(request.getQueryParams())
                          .build().toString();
    }

    @Override
    public HostAndPort getRemoteAddress() {
      return request.getRemoteAddress();
    }
  }

  private static class ServerResponseImpl implements ServerResponse {
    private final Response response;
    private final ServerRequest request;
    private final PathBinding pathBinding;

    public ServerResponseImpl(final Response response, final ServerRequest request, final PathBinding pathBinding) {
      this.response = response;
      this.request = request;
      this.pathBinding = pathBinding;
    }

    @Override
    public Optional<PathBinding> pathBinding() {
      return Optional.ofNullable(pathBinding);
    }

    @Override
    public ServerRequest getRequest() {
      return this.request;
    }

    @Override
    public Status getStatus() {
      return this.response.getStatus();
    }
  }


}
