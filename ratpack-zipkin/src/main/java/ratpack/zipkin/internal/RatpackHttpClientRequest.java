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

import com.github.kristofa.brave.http.HttpClientRequest;
import ratpack.http.client.RequestSpec;

import java.net.URI;

class RatpackHttpClientRequest implements HttpClientRequest {
  private final RequestSpec requestSpec;
  private final String method;

  RatpackHttpClientRequest(final RequestSpec requestSpec, final String method) {
    this.requestSpec = requestSpec;
    this.method = method;
  }

  @Override
  public void addHeader(final String header, final String value) {
    requestSpec.getHeaders().add(header, value);
  }

  @Override
  public URI getUri() {
    return requestSpec.getUrl();
  }

  @Override
  public String getHttpMethod() {
    return method;
  }
}
