package ratpack.zipkin.internal;

import brave.Span;
import brave.http.HttpClientHandler;
import brave.http.HttpTracing;
import brave.propagation.TraceContext;
import com.google.common.reflect.TypeToken;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.http.MutableHeaders;
import ratpack.http.client.HttpResponse;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.zipkin.ClientTracingInterceptor;

import javax.inject.Inject;

public class DefaultClientTracingInterceptor implements ClientTracingInterceptor {

  private static final TypeToken<Span> SpanToken = new TypeToken<Span>() {};

  private final HttpClientHandler<RequestSpec, Integer> handler;
  private final TraceContext.Injector<MutableHeaders> injector;
  private final Execution execution;

  @Inject
  public DefaultClientTracingInterceptor(final HttpTracing httpTracing, final Execution execution) {
    this.execution = execution;
    this.handler = HttpClientHandler.create(httpTracing, new ClientHttpAdapter());
    this.injector = httpTracing.tracing().propagation().injector(MutableHeaders::set);
  }

  @Override
  public void request(RequestSpec spec) {
    final Span span = this.handler.handleSend(injector, spec.getHeaders(), spec);
    this.execution.add(SpanToken, span);
    spec.onRedirect(
      res -> redirectHandler(res, span)
    );
  }

  private Action<? super RequestSpec> redirectHandler(ReceivedResponse response, Span span) {
    return (spec) -> handler.handleReceive(response.getStatusCode(), null, span);
  }

  @Override
  public void response(HttpResponse response) {
    this.execution
      .maybeGet(SpanToken)
      .ifPresent(s -> this.handler.handleReceive(response.getStatusCode(), null, s));
  }

  @Override
  public void error(Throwable e) {
    this.execution
      .maybeGet(SpanToken)
      .ifPresent(s -> this.handler.handleReceive(null, e, s));
  }
}
