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
package ratpack.zipkin;

import com.github.kristofa.brave.ClientRequestInterceptor;
import com.github.kristofa.brave.ClientResponseInterceptor;
import io.netty.buffer.ByteBufAllocator;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;
import ratpack.zipkin.internal.ClientRequestAdapterFactory;
import ratpack.zipkin.internal.ClientResponseAdapterFactory;

import javax.inject.Inject;
import java.net.URI;
import java.time.Duration;

/**
 * Decorator that adds Zipkin client logging around {@link HttpClient}.
 */
public class ZipkinHttpClient implements HttpClient {
  private final HttpClient delegate;
  private final ClientRequestInterceptor requestInterceptor;
  private final ClientResponseInterceptor responseInterceptor;
  private final ClientRequestAdapterFactory requestAdapterFactory;
  private final ClientResponseAdapterFactory responseAdapterFactory;

  @Inject
  public ZipkinHttpClient(final HttpClient delegate,
                          final ClientRequestInterceptor requestInterceptor,
                          final ClientResponseInterceptor responseInterceptor,
                          final ClientRequestAdapterFactory requestAdapterFactory,
                          final ClientResponseAdapterFactory responseAdapterFactory) {
    this.delegate = delegate;
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
    this.requestAdapterFactory = requestAdapterFactory;
    this.responseAdapterFactory = responseAdapterFactory;
  }

  @Override
  public Promise<ReceivedResponse> get(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer.prepend(requestSpec ->
        requestInterceptor.handle(requestAdapterFactory.createAdaptor(requestSpec, "GET"))
    ));
  }

  @Override
  public Promise<ReceivedResponse> get(final URI uri) {
    return get(uri, Action.noop());
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
  public Promise<ReceivedResponse> post(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer.prepend(requestSpec ->
        requestInterceptor.handle(requestAdapterFactory.createAdaptor(requestSpec, "POST"))
    ));
  }

  @Override
  public Promise<ReceivedResponse> request(final URI uri, final Action<? super RequestSpec>
      requestConfigurer) {
    return delegate
        .request(uri, requestConfigurer)
        .wiretap(receivedResponseResult ->
            responseInterceptor.handle(responseAdapterFactory.createAdapter(receivedResponseResult.getValue())));
  }

  @Override
  public Promise<StreamedResponse> requestStream(final URI uri, final Action<? super RequestSpec>
      requestConfigurer) {
    return delegate.requestStream(uri, requestConfigurer);
  }
}
