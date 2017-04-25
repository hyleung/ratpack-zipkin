package ratpack.zipkin.internal;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import javax.inject.Inject;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.HttpMethod;
import ratpack.http.Request;
import ratpack.zipkin.ServerTracingHandler;
import ratpack.zipkin.SpanNameProvider;
import zipkin.Constants;
import zipkin.TraceKeys;

/**
 * {@link Handler} for ZipKin tracing.
 */
public final class DefaultServerTracingHandler implements ServerTracingHandler {

  private final Tracing tracing;
  private final SpanNameProvider spanNameProvider;
  private final TraceContext.Extractor<Request> extractor;

  @Inject
  public DefaultServerTracingHandler(Tracing tracing, SpanNameProvider spanNameProvider) {
    this.tracing = tracing;
    this.spanNameProvider = spanNameProvider;
    this.extractor = tracing.propagation().extractor((Request r, String name) -> r.getHeaders().get(name));
  }

  @Override
  public void handle(Context ctx) throws Exception {

    TraceContextOrSamplingFlags contextOrSamplingFlags = extractor.extract(ctx.getRequest());

    final Span span = (contextOrSamplingFlags.context() != null
        ? tracing.tracer().joinSpan(contextOrSamplingFlags.context())
        : tracing.tracer().newTrace(contextOrSamplingFlags.samplingFlags()))
        .name(spanNameProvider.getName(new DefaultRequestSpanNameAdapter(ctx.getRequest())))
        .kind(Span.Kind.SERVER);

    Request request = ctx.getRequest();
    span.tag(TraceKeys.HTTP_URL, request.getUri());

    ctx.getResponse().beforeSend(response -> {
      if (response != null && response.getStatus() != null) {
        int status = response.getStatus().getCode();
        if (status < 200 || status > 299) {
          span.tag(TraceKeys.HTTP_STATUS_CODE, String.valueOf(status));
        }
        if (status > 399) {
          span.tag(Constants.ERROR, "server error " + response.getStatus().getCode());
        }
      } else {
        span.tag(Constants.ERROR, "missing or unknown status code");
      }

      span.finish();
    });

    try(Tracer.SpanInScope ws = tracing.tracer().withSpanInScope(span)) {
      span.start();
      ctx.next();
    }
  }


  private static final class DefaultRequestSpanNameAdapter implements
      SpanNameProvider.SpanNameProviderAdapter {

    private final Request request;

    DefaultRequestSpanNameAdapter(Request request) {
      this.request = request;
    }

    @Override
    public String getUri() {
      return this.request.getUri();
    }

    @Override
    public HttpMethod getMethod() {
      return this.request.getMethod();
    }
  }
}
