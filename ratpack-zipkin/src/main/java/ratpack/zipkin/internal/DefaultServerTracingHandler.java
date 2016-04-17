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
    private final SpanNameProvider spanNameProvider;
    private final RequestAnnotationExtractor requestAnnotationExtractor;
    private final ResponseAnnotationExtractor responseAnnotationExtractor;

    @Inject
    public DefaultServerTracingHandler(final ServerRequestInterceptor requestInterceptor,
                         final ServerResponseInterceptor responseInterceptor,
                         final ServerRequestAdapterFactory requestAdapterFactory,
                         final ServerResponseAdapterFactory responseAdapterFactory,
                         final SpanNameProvider spanNameProvider,
                         final RequestAnnotationExtractor requestAnnotationExtractor,
                         final ResponseAnnotationExtractor responseAnnotationExtractor) {
        this.requestInterceptor = requestInterceptor;
        this.responseInterceptor = responseInterceptor;
        this.requestAdapterFactory = requestAdapterFactory;
        this.responseAdapterFactory = responseAdapterFactory;
        this.spanNameProvider = spanNameProvider;
        this.requestAnnotationExtractor = requestAnnotationExtractor;
        this.responseAnnotationExtractor = responseAnnotationExtractor;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        ServerRequestAdapter requestAdapter = requestAdapterFactory.createAdapter(spanNameProvider,
                ctx.getRequest(), requestAnnotationExtractor);
        requestInterceptor.handle(requestAdapter);
        ctx.getResponse()
                .beforeSend(response -> responseInterceptor
                        .handle(responseAdapterFactory.createAdapter(response, responseAnnotationExtractor)));
        ctx.next();
    }
}
