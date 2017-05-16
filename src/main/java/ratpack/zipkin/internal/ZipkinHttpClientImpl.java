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
import brave.http.HttpClientHandler;
import brave.http.HttpTracing;
import brave.propagation.TraceContext;
import io.netty.buffer.ByteBufAllocator;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
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
    final HttpClientHandler<WrappedRequestSpec, Integer> handler;
    final TraceContext.Injector<MutableHeaders> injector;

    @Inject
    public ZipkinHttpClientImpl(final HttpClient delegate, final HttpTracing httpTracing) {
        this.delegate = delegate;
        this.handler = HttpClientHandler.create(httpTracing, new HttpAdapter());
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
    public int getMaxContentLength() {
        return delegate.getMaxContentLength();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public Promise<ReceivedResponse> request(URI uri, Action<? super RequestSpec> action) {
        AtomicReference<Span> span = new AtomicReference<>();
        return delegate
            .request(uri, action.append(requestSpec -> {
                WrappedRequestSpec captor = new WrappedRequestSpec(this.handler, this.injector, requestSpec, span);
                action.execute(captor);
            }))
            .wiretap(response -> responseWithSpan(response, span));
    }

    @Override
    public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> action) {
        AtomicReference<Span> span = new AtomicReference<>();
        return delegate
            .requestStream(uri, action.append(requestSpec -> {
                WrappedRequestSpec captor = new WrappedRequestSpec(this.handler, this.injector, requestSpec, span);
                // streamed request doesn't set the http method.
                // start span here until a better solution presents itself.
                span.set(this.handler.handleSend(this.injector, captor.getHeaders(), captor));
                action.execute(captor);
            }))
            .wiretap(response -> streamedResponseWithSpan(response, span));
    }

    @Override
    public Promise<ReceivedResponse> get(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
        return request(uri, requestConfigurer.prepend(RequestSpec::get));
    }

    @Override
    public Promise<ReceivedResponse> post(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
        return request(uri, requestConfigurer.prepend(RequestSpec::post));
    }

    private void streamedResponseWithSpan(Result<StreamedResponse> response, AtomicReference<Span> ref) {
        Span span = ref.get();
        if (span == null) return;

        Integer statusCode = (response.isError() || response.getValue() == null)
            ? null : response.getValue().getStatusCode();

        handler.handleReceive(statusCode, response.getThrowable(), span);
    }

    private void responseWithSpan(Result<ReceivedResponse> response, AtomicReference<Span> ref) {
        Span span = ref.get();
        if (span == null) return;

        Integer statusCode = (response.isError() || response.getValue() == null)
            ? null : response.getValue().getStatusCode();

        handler.handleReceive(statusCode, response.getThrowable(), span);
    }

    static final class HttpAdapter
        extends brave.http.HttpClientAdapter<WrappedRequestSpec, Integer> {

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

        @Override public Integer statusCode(Integer response) {
            return response;
        }
    }

    /**
     * RequestSpec wrapper that captures the method type, sets up redirect handling
     * and starts new spans when a method type is set.
     */
    static final class WrappedRequestSpec implements RequestSpec {

        private final RequestSpec delegate;
        private final AtomicReference<Span> span;
        private final HttpClientHandler<WrappedRequestSpec, Integer> handler;
        private final TraceContext.Injector<MutableHeaders> injector;
        private HttpMethod capturedMethod;

        WrappedRequestSpec(HttpClientHandler<WrappedRequestSpec, Integer> handler,
            TraceContext.Injector<MutableHeaders> injector, RequestSpec spec,  AtomicReference<Span> span) {
            this.delegate = spec;
            this.span = span;
            this.handler = handler;
            this.injector = injector;
            this.delegate.onRedirect(this::redirectHandler);
        }

        /**
         * Default redirect handler that ensures the span is marked as received before
         * a new span is created.
         *
         * @param response
         * @return
         */
        private Action<? super RequestSpec> redirectHandler(ReceivedResponse response) {
            handler.handleReceive(response.getStatusCode(), null, span.get());
            return (s) -> new WrappedRequestSpec(handler, injector, s, span);
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
        public MutableHeaders getHeaders() {
            return this.delegate.getHeaders();
        }

        @Override
        public RequestSpec maxContentLength(int numBytes) {
            this.delegate.maxContentLength(numBytes);
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
            span.set(handler.handleSend(injector, this.getHeaders(), this));
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
}
