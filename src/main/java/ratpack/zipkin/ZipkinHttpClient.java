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

import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.HttpMethod;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;

import java.net.URI;

/**
 * This API is modelled after {@link ratpack.http.client.HttpClient}, provides
 * *separate* methods for each of the supported HTTP methods.
 *
 * This class is a workaround for the lack of client request/response interceptors
 * + the API of {@link RequestSpec}, which doesn't provide any way to determine
 * the HTTP method of the request spec.
 */
public interface ZipkinHttpClient {
  /**
   * Execute an HTTP GET.
   * @param uri the URI
   * @param requestConfigurer the request configurer
   *
   * @return a Promise of a ReceivedResponse
   */
  Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> requestConfigurer);

  /**
   * Execute an HTTP GET.
   *
   * With not additional request configuration.
   * @param uri the URI
   *
   * @return a Promise of a ReceivedResponse
   */
  default Promise<ReceivedResponse> get(final URI uri) {
    return get(uri, Action.noop());
  }
  /**
   * Execute an HTTP GET.
   * @param uri the URI
   * @param requestConfigurer the request configurer
   *
   * @return a Promise of a ReceivedResponse
   */
  Promise<ReceivedResponse> post(URI uri, Action<? super RequestSpec> requestConfigurer);
  /**
   * Execute an HTTP POST.
   * @param uri the URI
   * @param requestConfigurer the request configurer
   *
   * @return a Promise of a ReceivedResponse
   */
  Promise<ReceivedResponse> put(URI uri, Action<? super RequestSpec> requestConfigurer);
  /**
   * Execute an HTTP PUT.
   * @param uri the URI
   * @param requestConfigurer the request configurer
   *
   * @return a Promise of a ReceivedResponse
   */
  Promise<ReceivedResponse> delete(URI uri, Action<? super RequestSpec> requestConfigurer);
  /**
   * Execute an HTTP PATCH.
   * @param uri the URI
   * @param requestConfigurer the request configurer
   *
   * @return a Promise of a ReceivedResponse
   */
  Promise<ReceivedResponse> patch(URI uri, Action<? super RequestSpec> requestConfigurer);
  /**
   * Execute an HTTP HEAD.
   * @param uri the URI
   * @param requestConfigurer the request configurer
   *
   * @return a Promise of a ReceivedResponse
   */
  Promise<ReceivedResponse> head(URI uri, Action<? super RequestSpec> requestConfigurer);
  /**
   * Execute an HTTP OPTIONS.
   * @param uri the URI
   * @param requestConfigurer the request configurer
   *
   * @return a Promise of a ReceivedResponse
   */
  Promise<ReceivedResponse> options(URI uri, Action<? super RequestSpec> requestConfigurer);
  /**
   * Execute an HTTP request via Ratpack HttpClient's requestStream API.
   * @param uri the URI
   * @param method the Http Method being invoked
   * @param requestConfigurer the request configurer
   * @return a Promise of a StreamedResponse
   */
  Promise<StreamedResponse> requestStream(URI uri, HttpMethod method, Action<? super RequestSpec> requestConfigurer);
}
