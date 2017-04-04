package ratpack.zipkin.internal;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
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

  private static final TraceContext.Extractor<Request> extractor =
      Propagation.B3_STRING.extractor((Request r, String name) -> r.getHeaders().get(name));

  private final Tracer tracer;
  private final SpanNameProvider spanNameProvider;

  @Inject
  public DefaultServerTracingHandler(Tracer tracer, SpanNameProvider spanNameProvider) {
    this.tracer = tracer;
    this.spanNameProvider = spanNameProvider;
  }

  @Override
  public void handle(Context ctx) throws Exception {

    TraceContextOrSamplingFlags contextOrSamplingFlags = extractor.extract(ctx.getRequest());

    final Span span = (contextOrSamplingFlags.context() != null
        ? tracer.joinSpan(contextOrSamplingFlags.context())
        : tracer.newTrace(contextOrSamplingFlags.samplingFlags()))
        .name(spanNameProvider.getName(new DefaultRequestSpanNameAdapter(ctx.getRequest())));

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

    try(Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
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
