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
import io.netty.buffer.ByteBufAllocator;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import javax.inject.Inject;
import ratpack.api.Nullable;
import ratpack.exec.Promise;
import ratpack.exec.Result;
import ratpack.func.Action;
import ratpack.http.HttpMethod;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;
import ratpack.zipkin.SpanNameProvider;
import zipkin.Constants;
import zipkin.TraceKeys;

/**
 * Decorator that adds Zipkin client logging around {@link HttpClient}.
 */
public class ZipkinHttpClientImpl implements HttpClient {

    private final HttpClient delegate;
    private final Tracer tracer;
    private final SpanNameProvider spanNameProvider;

    @Inject
    public ZipkinHttpClientImpl(final HttpClient delegate, final Tracer tracer, final
        SpanNameProvider spanNameProvider) {
        this.delegate = delegate;
        this.tracer = tracer;
        this.spanNameProvider = spanNameProvider;
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
        final Span span = tracer.nextSpan();
        try(Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.start();
            return delegate
                .request(uri, actionWithSpan(action, span))
                .wiretap(response -> responseWithSpan(response, span));
        } // span.finish() is called after the response is handled.
    }

    @Override
    public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> requestConfigurer) {
        final Span span = tracer.nextSpan();
        try(Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.start();
            return delegate
                .requestStream(uri, actionWithSpan(requestConfigurer, span))
                .wiretap(response -> streamedResponseWithSpan(response, span));
        } // span.finish() is called after the response is handled.
    }

    @Override
    public Promise<ReceivedResponse> get(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
        return request(uri, requestConfigurer.prepend(RequestSpec::get));
    }

    @Override
    public Promise<ReceivedResponse> post(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
        return request(uri, requestConfigurer.prepend(RequestSpec::post));
    }

    private Action<? super RequestSpec> actionWithSpan(final Action<? super RequestSpec> action, final Span span) {
        return (request -> {
            MethodCapturingRequestSpec captor = new MethodCapturingRequestSpec(request);
            span.start();
            action.execute(captor);
            span.name(spanNameProvider.getName(new DefaultRequestSpecNameAdapter(captor)));
            span.tag(TraceKeys.HTTP_URL, captor.getUri().toString());
        });
    }

    private void streamedResponseWithSpan(Result<StreamedResponse> response, Span span) {
        resultWithSpan(response, () -> response.getValue().getStatusCode(), span);
    }

    private void responseWithSpan(Result<ReceivedResponse> response, Span span) {
        resultWithSpan(response, () -> response.getValue().getStatusCode(), span);
    }

    private void resultWithSpan(Result<?> result, StatusCode statusCode, Span span) {
        if (result.isError()) {
            String message = result.getThrowable().getMessage();
            if (message != null) {
                span.tag(Constants.ERROR, result.getThrowable().getClass().getSimpleName());
            }
        } else {
            int status = statusCode.getStatusCode();
            if (status < 200 || status > 399) {
                span.tag(TraceKeys.HTTP_STATUS_CODE, String.valueOf(status));
            } else if (status > 499) {
                span.tag(Constants.ERROR, "server error " + status);
            }
        }

        span.finish();
    }

    private interface StatusCode {
        int getStatusCode();
    }

    private static final class DefaultRequestSpecNameAdapter implements
        SpanNameProvider.SpanNameProviderAdapter {
        private final MethodCapturingRequestSpec request;

        DefaultRequestSpecNameAdapter(MethodCapturingRequestSpec request) {
            this.request = request;
        }

        @Override
        public String getUri() {
            return this.request.getUri().toString();
        }

        @Override
        public HttpMethod getMethod() {
            return Optional.ofNullable(request.getCapturedMethod()).orElse(HttpMethod.GET);
        }
    }

}
