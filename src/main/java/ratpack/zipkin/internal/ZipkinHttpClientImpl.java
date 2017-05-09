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
import brave.Tracer;
import brave.http.HttpClientHandler;
import brave.http.HttpTracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import io.netty.buffer.ByteBufAllocator;
import java.net.URI;
import java.time.Duration;
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
public class ZipkinHttpClientImpl implements HttpClient {

    private final HttpClient delegate;
    final HttpClientHandler<WrappedRequestSpec, Integer> handler;
    final TraceContext.Injector<MutableHeaders> injector;
    final Tracer tracer;
    final CurrentTraceContext context;

    @Inject
    public ZipkinHttpClientImpl(final HttpClient delegate, final HttpTracing httpTracing) {
        this.delegate = delegate;
        this.handler = HttpClientHandler.create(httpTracing, new HttpAdapter());
        this.injector = httpTracing.tracing().propagation().injector(MutableHeaders::set);
        this.tracer = httpTracing.tracing().tracer();
        this.context = httpTracing.tracing().currentTraceContext();
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
        TraceContext current = context.get();
        return delegate
            .request(uri, actionWithSpan(action, span, current))
            .wiretap(response -> responseWithSpan(response, span));
    }

    @Override
    public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> requestConfigurer) {
        AtomicReference<Span> span = new AtomicReference<>();
        TraceContext current = context.get();
        return delegate
            .requestStream(uri, actionWithSpan(requestConfigurer, span, current))
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

    private Action<? super RequestSpec> actionWithSpan(final Action<? super RequestSpec> action, final AtomicReference<Span> span, final TraceContext current) {
        return action.append(request -> {
            WrappedRequestSpec captor = new WrappedRequestSpec(this, request, span);
            action.execute(captor);
        });
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
            return request.getCapturedMethod().getName();
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
     * "Dummy" implementation of RequestSpec, used to capture the HTTP request method.
     *
     */
    static final class WrappedRequestSpec implements RequestSpec {

        private final RequestSpec delegate;
        private final ZipkinHttpClientImpl client;
        private final AtomicReference<Span> span;
        private HttpMethod capturedMethod;

        WrappedRequestSpec(ZipkinHttpClientImpl client, RequestSpec spec,  AtomicReference<Span> span) {
            this.delegate = spec;
            this.client = client;
            this.span = span;
        }

        @Override
        public RequestSpec redirects(int maxRedirects) {
            this.delegate.redirects(maxRedirects);
            return this;
        }

        @Override
        public RequestSpec onRedirect(Function<? super ReceivedResponse, Action<? super RequestSpec>> function) {
            this.delegate.onRedirect(function);
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
            span.set(client.handler.handleSend(client.injector, getHeaders(), this));
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
