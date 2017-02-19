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

import com.github.kristofa.brave.ClientRequestInterceptor;
import com.github.kristofa.brave.ClientResponseInterceptor;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.HttpMethod;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;
import ratpack.zipkin.ZipkinHttpClient;

import javax.inject.Inject;
import java.net.URI;

/**
 * Decorator that adds Zipkin client logging around {@link HttpClient}.
 */
public class ZipkinHttpClientImpl implements ZipkinHttpClient {
  private final HttpClient delegate;
  private final ClientRequestInterceptor requestInterceptor;
  private final ClientResponseInterceptor responseInterceptor;
  private final ClientRequestAdapterFactory requestAdapterFactory;
  private final ClientResponseAdapterFactory responseAdapterFactory;

  @Inject
  public ZipkinHttpClientImpl(final HttpClient delegate,
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
    return request(uri, requestConfigurer.append(requestSpec ->
        requestInterceptor
            .handle(requestAdapterFactory.createAdaptor(requestSpec, HttpMethod.GET.getName()))
    ));
  }


  @Override
  public Promise<ReceivedResponse> post(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer.append(requestSpec ->
        requestInterceptor
            .handle(requestAdapterFactory.createAdaptor(requestSpec.post(), HttpMethod.POST.getName()))
    ));
  }

  @Override
  public Promise<ReceivedResponse> put(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer.append(requestSpec ->
        requestInterceptor
            .handle(requestAdapterFactory.createAdaptor(requestSpec.put(), HttpMethod.PUT.getName()))
    ));
  }

  @Override
  public Promise<ReceivedResponse> delete(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer.append(requestSpec ->
        requestInterceptor
            .handle(requestAdapterFactory.createAdaptor(requestSpec.delete(), HttpMethod.DELETE.getName()))
    ));
  }

  @Override
  public Promise<ReceivedResponse> patch(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer.append(requestSpec ->
        requestInterceptor
            .handle(requestAdapterFactory.createAdaptor(requestSpec.patch(), HttpMethod.PATCH.getName()))
    ));
  }

  @Override
  public Promise<ReceivedResponse> head(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer.append(requestSpec ->
        requestInterceptor
            .handle(requestAdapterFactory.createAdaptor(requestSpec.head(), HttpMethod.HEAD.getName()))
    ));
  }

  @Override
  public Promise<ReceivedResponse> options(final URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer.append(requestSpec ->
        requestInterceptor
            .handle(requestAdapterFactory.createAdaptor(requestSpec.options(), HttpMethod.OPTIONS.getName()))
    ));
  }

  @Override
  public Promise<StreamedResponse> requestStream(URI uri, HttpMethod method, final Action<? super RequestSpec> requestConfigurer) {

    final Action<? super RequestSpec> wrappedRequestSpec = requestConfigurer.append(requestSpec ->
      requestInterceptor
        .handle(requestAdapterFactory.createAdaptor(requestSpec.method(method), method.getName()))
    );

    return delegate
        .requestStream(uri, wrappedRequestSpec)
        .wiretap(streamedResponseResult ->
              responseInterceptor.handle(responseAdapterFactory.createAdapter(streamedResponseResult.getValue()))
        );
  }

  private Promise<ReceivedResponse> request(final URI uri, final Action<? super RequestSpec>
      requestConfigurer) {
    return delegate
        .request(uri, requestConfigurer)
        .wiretap(receivedResponseResult ->
            responseInterceptor.handle(responseAdapterFactory.createAdapter(receivedResponseResult.getValue())));
  }
}
