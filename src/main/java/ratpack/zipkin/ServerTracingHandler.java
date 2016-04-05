package ratpack.zipkin;

import com.github.kristofa.brave.ServerRequestAdapter;
import com.github.kristofa.brave.ServerRequestInterceptor;
import com.github.kristofa.brave.ServerResponseInterceptor;
import com.github.kristofa.brave.http.ServiceNameProvider;
import com.github.kristofa.brave.http.SpanNameProvider;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.zipkin.internal.RatpackServerResponseAdapter;
import ratpack.zipkin.internal.RatpackServerRequestAdapter;

import javax.inject.Inject;

/**
 * Created by hyleung on 2016-04-03.
 */
public class ServerTracingHandler implements Handler {
  private final ServerRequestInterceptor requestInterceptor;
  private final ServerResponseInterceptor responseInterceptor;
  private final ServiceNameProvider serviceNameProvider;
  private final SpanNameProvider spanNameProvider;

  @Inject
  public ServerTracingHandler(final ServerRequestInterceptor requestInterceptor,
                              final ServerResponseInterceptor responseInterceptor,
                              final ServiceNameProvider serviceNameProvider,
                              final SpanNameProvider spanNameProvider) {
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
    this.serviceNameProvider = serviceNameProvider;
    this.spanNameProvider = spanNameProvider;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    ServerRequestAdapter requestAdapter =
        new RatpackServerRequestAdapter(spanNameProvider, ctx.getRequest());
    requestInterceptor.handle(requestAdapter);
    ctx.getResponse()
       .beforeSend(response -> responseInterceptor
           .handle(new RatpackServerResponseAdapter(response)));
    ctx.next();
  }
}
