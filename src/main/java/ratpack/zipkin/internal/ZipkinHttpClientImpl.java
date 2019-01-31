/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ratpack.zipkin.internal;

import brave.Span;
import brave.http.HttpClientAdapter;
import brave.http.HttpClientHandler;
import brave.http.HttpSampler;
import brave.http.HttpTracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.SamplingFlags;
import brave.propagation.ThreadLocalSpan;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import io.netty.buffer.ByteBufAllocator;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import io.netty.handler.ssl.SslContext;
import ratpack.exec.Promise;
import ratpack.exec.Result;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.http.HttpMethod;
import ratpack.http.MutableHeaders;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;

/**
 * Decorator that adds Zipkin client logging around {@link HttpClient}.
 */
public final class ZipkinHttpClientImpl implements HttpClient {

    private final HttpClient delegate;
    private final CurrentTraceContext currentTraceContext;
    private final ThreadLocalSpan threadLocalSpan;
    private final BiFunction<WrappedRequestSpec, TraceContext, Span> nextThreadLocalSpan;
    private final HttpClientHandler<WrappedRequestSpec, Integer> handler;
    private final TraceContext.Injector<MutableHeaders> injector;

    @Inject
    public ZipkinHttpClientImpl(final HttpClient delegate, final HttpTracing httpTracing) {
        this.delegate = delegate;
        this.threadLocalSpan = ThreadLocalSpan.create(httpTracing.tracing().tracer());
        this.currentTraceContext = httpTracing.tracing().currentTraceContext();
        this.nextThreadLocalSpan = new NextSpan(threadLocalSpan, httpTracing.clientSampler());
        this.handler = HttpClientHandler.create(httpTracing, ADAPTER);
        this.injector = httpTracing.tracing().propagation().injector(MutableHeaders::set);
    }

    @Override
    public ByteBufAllocator getByteBufAllocator() {
        return delegate.getByteBufAllocator();
    }

    @Override
    public int getPoolSize() {
        return delegate.getPoolSize();
    }

    @Override
    public Duration getReadTimeout() {
        return delegate.getReadTimeout();
    }

    @Override
    public Duration getConnectTimeout() {
        return delegate.getConnectTimeout();
    }

    @Override
    public int getMaxContentLength() {
        return delegate.getMaxContentLength();
    }

    @Override
    public int getMaxResponseChunkSize() {
        return delegate.getMaxResponseChunkSize();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public Promise<ReceivedResponse> request(URI uri, Action<? super RequestSpec> action) {
        // save off the current span as the parent of a future client span
        TraceContext parent = currentTraceContext.get();
        // this reference is used to manually propagate the span from the request to the response
        // we use this because we cannot assume a thread context exists betweeen them.
        AtomicReference<Span> currentSpan = new AtomicReference<>();
        return delegate.request(uri, (RequestSpec requestSpec) -> {
            try {
                action.execute(new WrappedRequestSpec(requestSpec, parent, currentSpan));
            } finally {
                // moves the span from thread local context to an atomic ref the response can read
                currentSpan.set(threadLocalSpan.remove());
            }
        }).wiretap(response -> responseWithSpan(response, currentSpan.getAndSet(null)));
    }

    @Override
    public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> action) {
        // save off the current span as the parent of a future client span
        TraceContext parent = currentTraceContext.get();
        // this reference is used to manually propagate the span from the request to the response
        // we use this because we cannot assume a thread context exists betweeen them.
        AtomicReference<Span> currentSpan = new AtomicReference<>();
        return delegate.requestStream(uri, (RequestSpec requestSpec) -> {
            // streamed request doesn't set the http method.
            // start span here until a better solution presents itself.
            WrappedRequestSpec captor = new WrappedRequestSpec(requestSpec, parent, currentSpan);

            Span span = nextThreadLocalSpan.apply(captor, parent);
            try {
                handler.handleSend(injector, captor.getHeaders(), captor, span);
                action.execute(new WrappedRequestSpec(requestSpec, parent, currentSpan));
            } finally {
                // moves the span from thread local context to an atomic ref the response can read
                currentSpan.set(threadLocalSpan.remove());
            }
        }).wiretap(response -> streamedResponseWithSpan(response, currentSpan.getAndSet(null)));
    }

    @Override
    public Promise<ReceivedResponse> get(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
        return request(uri, requestConfigurer.prepend(RequestSpec::get));
    }

    @Override
    public Promise<ReceivedResponse> post(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
        return request(uri, requestConfigurer.prepend(RequestSpec::post));
    }

    private void streamedResponseWithSpan(Result<StreamedResponse> response, Span currentSpan) {
        if (currentSpan == null) return;

        Integer statusCode = (response.isError() || response.getValue() == null)
            ? null : response.getValue().getStatusCode();

        handler.handleReceive(statusCode, response.getThrowable(), currentSpan);
    }

    private void responseWithSpan(Result<ReceivedResponse> response, Span currentSpan) {
        if (currentSpan == null) return;

        Integer statusCode = (response.isError() || response.getValue() == null)
            ? null : response.getValue().getStatusCode();

        handler.handleReceive(statusCode, response.getThrowable(), currentSpan);
    }

    static final HttpClientAdapter<WrappedRequestSpec, Integer> ADAPTER =
        new HttpClientAdapter<WrappedRequestSpec, Integer>() {

        @Override public String method(WrappedRequestSpec request) {
            HttpMethod method = Optional.ofNullable(request.getCapturedMethod()).orElse(HttpMethod.GET);
            return method.getName();
        }

        @Override public String path(WrappedRequestSpec request) {
            return request.getUri().getPath();
        }

        @Override public String url(WrappedRequestSpec request) {
            return request.getUri().toString();
        }

        @Override public String requestHeader(WrappedRequestSpec request, String name) {
            return request.getHeaders().get(name);
        }

        // integer because ReceivedResponse and StreamedResponse share no common interface
        @Override public Integer statusCode(Integer response) {
            return response;
        }
    };

    /**
     * RequestSpec wrapper that captures the method type, sets up redirect handling
     * and starts new spans when a method type is set.
     */
    // not a static type as it shares many references with the enclosing class
    final class WrappedRequestSpec implements RequestSpec {

        private final RequestSpec delegate;
        private final TraceContext parent;
        private final AtomicReference<Span> currentSpan;
        private HttpMethod capturedMethod;

        WrappedRequestSpec(
            RequestSpec delegate,
            TraceContext parent,
            AtomicReference<Span> currentSpan
        ) {
            this.delegate = delegate;
            this.parent = parent;
            this.currentSpan = currentSpan;
            this.delegate.onRedirect(this::redirectHandler);
        }

        /**
         * Default redirect handler that ensures the span is marked as received before
         * a new span is created.
         */
        private Action<? super RequestSpec> redirectHandler(ReceivedResponse response) {
            Span span = currentSpan.getAndSet(null);
            handler.handleReceive(response.getStatusCode(), null, span);
            return (s) -> new WrappedRequestSpec(s, parent, currentSpan);
        }

        @Override
        public RequestSpec redirects(int maxRedirects) {
            this.delegate.redirects(maxRedirects);
            return this;
        }

        @Override
        public RequestSpec onRedirect(Function<? super ReceivedResponse, Action<? super RequestSpec>> function) {

            Function<? super ReceivedResponse, Action<? super RequestSpec>> wrapped =
                (ReceivedResponse response) -> redirectHandler(response).append(function.apply(response));

            this.delegate.onRedirect(wrapped);
            return this;
        }

        @Override
        public RequestSpec sslContext(SSLContext sslContext) {
            this.delegate.sslContext(sslContext);
            return this;
        }

        @Override
        public RequestSpec sslContext(final SslContext sslContext) {
            this.delegate.sslContext(sslContext);
            return this;
        }

        @Override
        public MutableHeaders getHeaders() {
            return this.delegate.getHeaders();
        }

        @Override
        public RequestSpec maxContentLength(int numBytes) {
            this.delegate.maxContentLength(numBytes);
            return this;
        }

        @Override
        public RequestSpec responseMaxChunkSize(final int numBytes) {
            this.delegate.responseMaxChunkSize(numBytes);
            return this;
        }

        @Override
        public RequestSpec headers(Action<? super MutableHeaders> action) throws Exception {
            this.delegate.headers(action);
            return this;
        }

        @Override
        public RequestSpec method(HttpMethod method) {
            this.capturedMethod = method;
            Span currentSpan = nextThreadLocalSpan.apply(this, parent);
            handler.handleSend(injector, this.getHeaders(), this, currentSpan);
            this.delegate.method(method);
            return this;
        }

        @Override
        public RequestSpec decompressResponse(boolean shouldDecompress) {
            this.delegate.decompressResponse(shouldDecompress);
            return this;
        }

        @Override
        public URI getUri() {
            return this.delegate.getUri();
        }

        @Override
        public RequestSpec connectTimeout(Duration duration) {
            this.delegate.connectTimeout(duration);
            return this;
        }

        @Override
        public RequestSpec readTimeout(Duration duration) {
            this.delegate.readTimeout(duration);
            return this;
        }

        @Override
        public Body getBody() {
            return this.delegate.getBody();
        }

        @Override
        public RequestSpec body(Action<? super Body> action) throws Exception {
            this.delegate.body(action);
            return this;
        }

        public HttpMethod getCapturedMethod() {
            return capturedMethod;
        }
    }

    /** This is a partial function that applies the last parent when creating a new span */
    static class NextSpan implements BiFunction<WrappedRequestSpec, TraceContext, Span> {
        final ThreadLocalSpan span;
        final HttpSampler sampler;

        NextSpan(ThreadLocalSpan span, HttpSampler sampler) {
            this.span = span;
            this.sampler = sampler;
        }

        @Override public Span apply(WrappedRequestSpec req, TraceContext parent) {
            if (parent != null) return span.next(TraceContextOrSamplingFlags.create(parent));
            Boolean sampled = sampler.trySample(ADAPTER, req);
            return span.next(TraceContextOrSamplingFlags.create(
                new SamplingFlags.Builder().sampled(sampled).build())
            );
        }
    }
}
