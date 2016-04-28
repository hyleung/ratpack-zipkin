package ratpack.zipkin.internal;

import com.github.kristofa.brave.ServerRequestAdapter;
import com.github.kristofa.brave.ServerRequestInterceptor;
import com.github.kristofa.brave.ServerResponseInterceptor;
import com.github.kristofa.brave.http.SpanNameProvider;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.zipkin.RequestAnnotationExtractor;
import ratpack.zipkin.ResponseAnnotationExtractor;
import ratpack.zipkin.ServerTracingHandler;

import javax.inject.Inject;

/**
 * {@link Handler} for ZipKin tracing.
 */
public final class DefaultServerTracingHandler implements ServerTracingHandler {
    private final ServerRequestInterceptor requestInterceptor;
    private final ServerResponseInterceptor responseInterceptor;
    private final ServerRequestAdapterFactory requestAdapterFactory;
    private final ServerResponseAdapterFactory responseAdapterFactory;

    @Inject
    public DefaultServerTracingHandler(final ServerRequestInterceptor requestInterceptor,
                         final ServerResponseInterceptor responseInterceptor,
                         final ServerRequestAdapterFactory requestAdapterFactory,
                         final ServerResponseAdapterFactory responseAdapterFactory) {
        this.requestInterceptor = requestInterceptor;
        this.responseInterceptor = responseInterceptor;
        this.requestAdapterFactory = requestAdapterFactory;
        this.responseAdapterFactory = responseAdapterFactory;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        ServerRequestAdapter requestAdapter = requestAdapterFactory.createAdapter(ctx.getRequest());
        requestInterceptor.handle(requestAdapter);
        ctx.getResponse()
                .beforeSend(response -> responseInterceptor
                        .handle(responseAdapterFactory.createAdapter(response)));
        ctx.next();
    }
}
